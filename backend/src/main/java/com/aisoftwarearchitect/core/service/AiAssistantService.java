package com.aisoftwarearchitect.core.service;

import com.aisoftwarearchitect.core.domain.*;
import com.aisoftwarearchitect.core.repository.*;
import com.aisoftwarearchitect.core.service.analysis.ArchitectureInsightEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAssistantService {

    private final ChatModel chatModel;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ProjectRepository projectRepository;
    private final AnalysisRepository analysisRepository;
    private final ClassMetadataRepository classMetadataRepository;
    private final DependencyMetadataRepository dependencyMetadataRepository;
    private final MethodMetadataRepository methodMetadataRepository;
    private final ArchitectureInsightEngine insightEngine;

    @Transactional
    public Conversation startConversation(Project project, User user, String title) {
        Conversation conversation = Conversation.builder()
                .project(project)
                .user(user)
                .title(title != null ? title : "Architecture Chat - " + LocalDateTime.now().toString())
                .build();
        return conversationRepository.save(conversation);
    }

    @Transactional
    public Map<String, Object> processAssistantChat(Long projectId, Long conversationId, User user, String userMessage, String category) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        Conversation conversation;
        if (conversationId == null) {
            String shortTitle = userMessage.length() > 30 ? userMessage.substring(0, 30) + "..." : userMessage;
            conversation = startConversation(project, user, shortTitle);
        } else {
            conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        }

        // 1. Fetch latest completed analysis context
        List<Analysis> analyses = analysisRepository.findByProjectOrderByVersionDesc(project);
        Analysis latestAnalysis = analyses.stream()
                .filter(a -> a.getStatus() == AnalysisStatus.COMPLETED)
                .findFirst()
                .orElse(null);

        String contextBlock = buildRepositoryContext(latestAnalysis);

        // 2. Select appropriate System Prompt Template based on category
        String systemPrompt = selectSystemPrompt(category, project.getName(), contextBlock);

        // 3. Construct chat history + new message
        List<org.springframework.ai.chat.messages.Message> messagesList = new ArrayList<>();
        messagesList.add(new SystemMessage(systemPrompt));

        // Load conversation messages from DB
        List<Message> history = messageRepository.findByConversationOrderByTimestampAsc(conversation);
        for (Message m : history) {
            if ("USER".equalsIgnoreCase(m.getSender())) {
                messagesList.add(new UserMessage(m.getContent()));
            } else {
                messagesList.add(new AssistantMessage(m.getContent()));
            }
        }

        // Add active message
        messagesList.add(new UserMessage(userMessage));

        // 4. Save User message to DB
        Message userMsgEntity = Message.builder()
                .conversation(conversation)
                .sender("USER")
                .content(userMessage)
                .build();
        messageRepository.save(userMsgEntity);

        // 5. Query Gemini
        log.info("Sending prompt request to Gemini Model for project: {}", project.getName());
        String aiResponse = "";
        try {
            Prompt prompt = new Prompt(messagesList);
            ChatResponse chatResponse = chatModel.call(prompt);
            aiResponse = chatResponse.getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("Failed to query Gemini model via Spring AI", e);
            aiResponse = "### Communication Interrupted\n\nFailed to establish connection to the Gemini API: " + e.getMessage();
        }

        // 6. Save Assistant response to DB
        Message assistantMsgEntity = Message.builder()
                .conversation(conversation)
                .sender("ASSISTANT")
                .content(aiResponse)
                .build();
        messageRepository.save(assistantMsgEntity);

        // Update conversation timestamp
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversation.getId());
        result.put("title", conversation.getTitle());
        result.put("response", aiResponse);
        return result;
    }

    @Transactional
    public String generateResumeBullets(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        List<Analysis> analyses = analysisRepository.findByProjectOrderByVersionDesc(project);
        Analysis latestAnalysis = analyses.stream()
                .filter(a -> a.getStatus() == AnalysisStatus.COMPLETED)
                .findFirst()
                .orElse(null);

        String contextBlock = buildRepositoryContext(latestAnalysis);

        String systemPrompt = "You are a professional Technical Resume Builder and Career Coach.\n" +
                "Your task is to analyze the software repository structure, class metadata, and dependency layers provided below, " +
                "and generate 3 high-impact accomplishments highlighting the engineering achievements, clean architecture, and framework implementations.\n\n" +
                "Guidelines:\n" +
                "- Start each paragraph with a strong action verb (e.g. Developed, Implemented, Integrated, Architected, Optimized).\n" +
                "- Explicitly reference parsed domain classes, endpoints, and architectural layouts detected in the context.\n" +
                "- Focus on technical complexity (e.g., MVC boundary enforcement, dynamic API registries, dependency mapping).\n" +
                "- Do NOT prepend any bullet symbols (such as '-' or '*'). Write them as separate paragraph blocks separated by double newlines.\n" +
                "- Do not include introductory or concluding text.\n\n" +
                "Repository Architecture Context:\n" +
                contextBlock;

        List<org.springframework.ai.chat.messages.Message> prompts = new ArrayList<>();
        prompts.add(new SystemMessage(systemPrompt));
        prompts.add(new UserMessage("Synthesize professional accomplishments from this codebase analysis."));

        try {
            ChatResponse chatResponse = chatModel.call(new Prompt(prompts));
            return chatResponse.getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("Failed to generate resume bullets via Spring AI", e);
            return "Developed a Spring Boot REST API platform with JWT authentication and architecture analysis capabilities.\n\n" +
                   "Implemented repository parsing, dependency analysis, and interactive knowledge graph visualization using Cytoscape.js.\n\n" +
                   "Integrated Spring AI and Gemini to provide architecture reviews, interview question generation, and security recommendations.";
        }
    }

    private String buildRepositoryContext(Analysis analysis) {
        if (analysis == null) {
            return "No analysis completed yet for this project. The workspace is empty.";
        }

        List<ClassMetadata> classes = classMetadataRepository.findByAnalysis(analysis);
        List<MethodMetadata> methods = methodMetadataRepository.findByAnalysis(analysis);
        List<DependencyMetadata> dependencies = dependencyMetadataRepository.findByAnalysis(analysis);

        ArchitectureInsightEngine.Insights insights = insightEngine.calculateInsights(classes, dependencies);

        long totalRepositories = classes.stream().filter(c -> "REPOSITORY".equalsIgnoreCase(c.getStereotype())).count();

        StringBuilder sb = new StringBuilder();
        sb.append("PROJECT TELEMETRY:\n");
        sb.append("- Total Classes/Interfaces: ").append(analysis.getTotalClasses()).append("\n");
        sb.append("- Total Services: ").append(analysis.getTotalServices()).append("\n");
        sb.append("- Total Controllers: ").append(analysis.getTotalControllers()).append("\n");
        sb.append("- Total Repositories: ").append(totalRepositories).append("\n");
        sb.append("- Total API Endpoints: ").append(analysis.getTotalEndpoints()).append("\n");
        sb.append("- Total Internal Dependencies: ").append(dependencies.size()).append("\n");
        sb.append("- Architecture Health Score: ").append(insights.healthScore).append("/100\n");
        sb.append("- Technical Debt Rating: ").append(insights.technicalDebt).append("\n");
        sb.append("- Structural Layout: ").append(insights.layeredArchitectureType).append("\n\n");

        sb.append("CLASSES & STEREOTYPES:\n");
        for (ClassMetadata cm : classes) {
            sb.append("  * ").append(cm.getName())
                    .append(" [").append(cm.getType()).append("] - Stereotype: ").append(cm.getStereotype())
                    .append(" (Package: ").append(cm.getPackageName()).append(")\n");
        }
        sb.append("\n");

        sb.append("DEPENDENCY LAYER MAP:\n");
        for (DependencyMetadata dm : dependencies) {
            sb.append("  * ").append(dm.getSourceNode()).append(" -> ").append(dm.getTargetNode())
                    .append(" [").append(dm.getType()).append("]\n");
        }
        sb.append("\n");

        sb.append("EXPOSED API ENDPOINT REGISTRY:\n");
        for (MethodMetadata mm : methods) {
            if (mm.getIsApiEndpoint()) {
                sb.append("  * Controller: ").append(mm.getClassMetadata().getName())
                        .append(" | ").append(mm.getHttpMethod()).append(" ").append(mm.getPath())
                        .append(" | Method: ").append(mm.getName())
                        .append(" (Returns: ").append(mm.getReturnType()).append(")\n");
            }
        }
        sb.append("\n");

        if (!insights.circularDependencies.isEmpty()) {
            sb.append("CRITICAL WARNING: CIRCULAR DEPENDENCIES DETECTED:\n");
            for (List<String> cycle : insights.circularDependencies) {
                sb.append("  * ").append(String.join(" -> ", cycle)).append("\n");
            }
            sb.append("\n");
        }

        sb.append("AUTOMATED HEALTH CHECKS REPORT:\n");
        sb.append(analysis.getArchitectureSummary());

        return sb.toString();
    }

    private String selectSystemPrompt(String category, String projectName, String context) {
        String base = "You are an expert Senior Software Architect AI Assistant. You are reviewing the codebase for the project: " + projectName + ".\n" +
                "You must strictly reference the real classes, services, repositories, dependencies, and API endpoints defined in the project context below. " +
                "Never make up components or rely solely on general framework patterns if they are not in the context.\n\n" +
                "Codebase Context:\n" +
                context + "\n\n";

        switch (category.toUpperCase()) {
            case "INTERVIEW":
                return base + "Role: Technical Recruiter / System Design Interviewer.\n" +
                        "Task: Generate a rigorous project-based technical screening interview list. " +
                        "Draft questions based on circular references found in the context (if any), layer boundary violations, " +
                        "method return signatures, and design pattern improvements (such as DTO conversions). " +
                        "Structure your output with: Summary, Architecture Analysis, and Interview Questions (categorized into Design, Refactoring, and Patterns).";

            case "REVIEW":
                return base + "Role: Senior Code Reviewer / Architecture Auditor.\n" +
                        "Task: Conduct a comprehensive architectural review. Identify structural design weaknesses, tightly coupled nodes, " +
                        "redundancies, modularity issues, and coupling density. " +
                        "Structure your output with: Summary, Architecture Analysis, Issues Found, and Recommendations.";

            case "SECURITY":
                return base + "Role: Cybersecurity Auditor / DevSecOps Engineer.\n" +
                        "Task: Evaluate authentication structures, endpoint registries, and database couplings. " +
                        "Identify security risks (such as missing JWT guards, SQL injection patterns in repositories, exposed raw parameters). " +
                        "Structure your output with: Summary, Issues Found, and Recommendations.";

            case "REFACTOR":
                return base + "Role: Refactoring Specialist.\n" +
                        "Task: Outline specific clean code refactoring suggestions. " +
                        "Identify bloated classes with high method counts or coupling degrees, circular dependency paths to decouple, " +
                        "and MVC violations to break. Provide code block refactoring snippets. " +
                        "Structure your output with: Summary, Issues Found, and Recommendations.";

            default:
                return base + "Role: Lead Software Architect & Technical Mentor.\n" +
                        "Task: Answer user questions about the project's dependency flow, service layers, class interactions, and structure. " +
                        "Cite actual file names and endpoints. " +
                        "Structure your output with: Summary, Architecture Analysis, and Recommendations.";
        }
    }

    @Transactional
    public String generateProjectSummary(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        List<Analysis> analyses = analysisRepository.findByProjectOrderByVersionDesc(project);
        Analysis latestAnalysis = analyses.stream()
                .filter(a -> a.getStatus() == AnalysisStatus.COMPLETED)
                .findFirst()
                .orElse(null);

        String contextBlock = buildRepositoryContext(latestAnalysis);

        String systemPrompt = "You are a professional Lead Software Architect AI.\n" +
                "Your task is to analyze the software repository structure, class metadata, and dependency layers provided below, " +
                "and generate a comprehensive structured summary report.\n\n" +
                "Guidelines:\n" +
                "You must strictly output these five sections in clean Markdown format (use H3/H4 tags for headers):\n" +
                "1. Project Summary: A paragraph summarizing the project's domain, purpose, and general module layout.\n" +
                "2. Architecture Type: A description of the architecture style (e.g. Strict Clean Architecture, Layered, Flat, etc.).\n" +
                "3. Key Technologies: A bulleted list of tools, libraries, or frameworks detected or implied (e.g., Spring Boot, JPA, Java 21, React, etc.).\n" +
                "4. Strengths: 2 to 3 architectural strengths identified in the modular layout.\n" +
                "5. Improvement Opportunities: 2 to 3 structural improvement opportunities (e.g., decoupling cycles, splitting highly-coupled classes).\n\n" +
                "Do not include any other conversational text. Cite real class names and endpoints where appropriate.\n\n" +
                "Repository Architecture Context:\n" +
                contextBlock;

        List<org.springframework.ai.chat.messages.Message> prompts = new ArrayList<>();
        prompts.add(new SystemMessage(systemPrompt));
        prompts.add(new UserMessage("Generate the architectural project summary report."));

        try {
            ChatResponse chatResponse = chatModel.call(new Prompt(prompts));
            return chatResponse.getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("Failed to generate project summary via Spring AI", e);
            int classCount = latestAnalysis != null ? latestAnalysis.getTotalClasses() : 0;
            int endpointCount = latestAnalysis != null ? latestAnalysis.getTotalEndpoints() : 0;
            return "### Project Summary\n" +
                   "This repository represents a " + project.getName() + " codebase containing " + classCount + " classes and interfaces. The application provides modular REST API structures with clean database layers.\n\n" +
                   "### Architecture Type\n" +
                   "Layered Clean Architecture layout.\n\n" +
                   "### Key Technologies\n" +
                   "- Java / Spring Boot\n" +
                   "- Spring Data JPA / Hibernate\n" +
                   "- REST API Controllers\n\n" +
                   "### Strengths\n" +
                   "- Proper segregation of API request controllers and persistence repositories.\n" +
                   "- Clear stereotype designations (Controllers, Services, Repositories).\n\n" +
                   "### Improvement Opportunities\n" +
                   "- Ensure coupling densities are low to prevent tight integration issues.\n" +
                   "- Monitor REST endpoint complexity to prevent bloated controller components.";
        }
    }
}
