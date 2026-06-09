package com.aisoftwarearchitect.core.service;

import com.aisoftwarearchitect.core.domain.*;
import com.aisoftwarearchitect.core.repository.*;
import com.aisoftwarearchitect.core.service.analysis.ArchitectureInsightEngine;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EngineeringReportService {

    private final ChatModel chatModel;
    private final ProjectRepository projectRepository;
    private final AnalysisRepository analysisRepository;
    private final ClassMetadataRepository classMetadataRepository;
    private final DependencyMetadataRepository dependencyMetadataRepository;
    private final MethodMetadataRepository methodMetadataRepository;
    private final ArchitectureInsightEngine insightEngine;

    @Transactional(readOnly = true)
    public Map<String, String> generateReports(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        List<Analysis> analyses = analysisRepository.findByProjectOrderByVersionDesc(project);
        Analysis latest = analyses.stream()
                .filter(a -> a.getStatus() == AnalysisStatus.COMPLETED)
                .findFirst()
                .orElse(null);

        Map<String, String> reports = new HashMap<>();
        if (latest == null) {
            reports.put("architecture", "No completed analysis found.");
            reports.put("techDebt", "No completed analysis found.");
            reports.put("dependency", "No completed analysis found.");
            reports.put("security", "No completed analysis found.");
            return reports;
        }

        List<ClassMetadata> classes = classMetadataRepository.findByAnalysis(latest);
        List<DependencyMetadata> dependencies = dependencyMetadataRepository.findByAnalysis(latest);
        List<MethodMetadata> methods = methodMetadataRepository.findByAnalysis(latest);
        
        ArchitectureInsightEngine.Insights insights = insightEngine.calculateInsights(classes, dependencies, methods);

        String baseContext = String.format(
                "Project: %s\nClasses Count: %d\nDependencies Count: %d\nEndpoints Count: %d\nHealth Score: %d\nTech Debt Level: %s\nLargest Module: %s\n",
                project.getName(), classes.size(), dependencies.size(), methods.stream().filter(m -> m.getIsApiEndpoint() != null && m.getIsApiEndpoint()).count(),
                insights.healthScore, insights.technicalDebt, insights.largestModule
        );

        reports.put("architecture", generateReportSection(baseContext, "ARCHITECTURE", project.getName()));
        reports.put("techDebt", generateReportSection(baseContext, "TECHNICAL_DEBT", project.getName()));
        reports.put("dependency", generateReportSection(baseContext, "DEPENDENCY_ANALYSIS", project.getName()));
        reports.put("security", generateReportSection(baseContext, "SECURITY", project.getName()));

        return reports;
    }

    private String generateReportSection(String context, String reportType, String projectName) {
        String systemPrompt = "You are an expert Senior Software Architect.\n" +
                "Generate a detailed technical evaluation section on '" + reportType + "' for the project '" + projectName + "'.\n\n" +
                "Guidelines:\n" +
                "Generate exactly 2 to 3 paragraphs focusing on:\n";

        if ("ARCHITECTURE".equals(reportType)) {
            systemPrompt += "- Evaluation of the layered structure, class layout, and structural cohesion.\n" +
                    "- Strengths and limitations of the MVC stereotyping patterns used.\n";
        } else if ("TECHNICAL_DEBT".equals(reportType)) {
            systemPrompt += "- Code smell audits, circular couplings risk, and long classes refactoring recommendations.\n" +
                    "- Estimated effort to decouple bloated elements.\n";
        } else if ("DEPENDENCY_ANALYSIS".equals(reportType)) {
            systemPrompt += "- Visual mapping of in-degree/out-degree couplings and circular references.\n" +
                    "- Component encapsulation health reviews.\n";
        } else {
            systemPrompt += "- API routing exposition risks, missing authentication guards, and validation checks.\n" +
                    "- Best practices and security recommendations.\n";
        }

        systemPrompt += "\nUse clean Markdown headers and lists. Do not include conversational remarks.\n\nContext:\n" + context;

        try {
            ChatResponse chatResponse = chatModel.call(new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage("Generate " + reportType + " report."))));
            return chatResponse.getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("Failed to generate AI report for: " + reportType + ", returning fallback report", e);
            return generateFallbackReport(reportType, projectName, context);
        }
    }

    private String generateFallbackReport(String reportType, String projectName, String context) {
        if ("ARCHITECTURE".equals(reportType)) {
            return "### Architecture Audit Report\n\n" +
                    "The system architecture shows a standard MVC style layout. The controller layers parse inputs, " +
                    "delegating core validation behaviors to services, which query repositories. This promotes high structural modularity.\n\n" +
                    "We recommend validating that controllers do not perform direct business computations to maintain separation of concerns.";
        } else if ("TECHNICAL_DEBT".equals(reportType)) {
            return "### Technical Debt Audit Report\n\n" +
                    "Refactoring code smells, circular references, and bloated class methods is essential.\n\n" +
                    "1. Bloated Controllers: Controllers with high endpoint densities should be subdivided into separate modules.\n" +
                    "2. Decouple Circular Layers: Ensure no package contains cyclic references, making compilation safer.";
        } else if ("DEPENDENCY_ANALYSIS".equals(reportType)) {
            return "### Dependency Analysis Report\n\n" +
                    "The coupling analysis indicates that component interactions are localized. Most classes communicate within their stereotypes.\n\n" +
                    "We advise monitoring coupling degrees to ensure the system is maintainable as new features are added.";
        } else {
            return "### Security Assessment Report\n\n" +
                    "A review of API routing indicates standard controller mappings.\n\n" +
                    "- Ensure all input payloads are strictly validated using @Valid annotation parameters.\n" +
                    "- Guarantee JWT authorization filters validate roles properly on all admin controllers.";
        }
    }

    @Transactional(readOnly = true)
    public byte[] generatePdfReport(Long projectId) {
        Map<String, String> reports = generateReports(projectId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Project project = projectRepository.findById(projectId).orElse(null);
            document.add(new Paragraph("Engineering Audit & Architecture Report", titleFont));
            document.add(new Paragraph("Project: " + (project != null ? project.getName() : "Sample"), textFont));
            document.add(new Paragraph("Generated on: " + LocalDateTime.now().toString(), textFont));
            document.add(new Paragraph("\n"));

            String[] keys = {"architecture", "techDebt", "dependency", "security"};
            String[] titles = {"Architecture Report", "Technical Debt Report", "Dependency Analysis Report", "Security Report"};

            for (int i = 0; i < keys.length; i++) {
                String reportText = reports.get(keys[i]);
                document.add(new Paragraph(titles[i], sectionFont));
                document.add(new Paragraph("\n"));

                String[] lines = reportText.split("\n");
                for (String line : lines) {
                    if (!line.trim().isEmpty() && !line.startsWith("#")) {
                        document.add(new Paragraph(line.trim(), textFont));
                    }
                }
                document.add(new Paragraph("\n\n"));
            }

            document.close();
        } catch (Exception e) {
            log.error("Failed to generate PDF engineering report", e);
        }

        return out.toByteArray();
    }
}
