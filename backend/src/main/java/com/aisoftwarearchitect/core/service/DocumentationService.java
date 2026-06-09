package com.aisoftwarearchitect.core.service;

import com.aisoftwarearchitect.core.domain.*;
import com.aisoftwarearchitect.core.repository.*;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentationService {

    private final ChatModel chatModel;
    private final ProjectRepository projectRepository;
    private final AnalysisRepository analysisRepository;
    private final ClassMetadataRepository classMetadataRepository;
    private final DependencyMetadataRepository dependencyMetadataRepository;
    private final MethodMetadataRepository methodMetadataRepository;

    @Transactional(readOnly = true)
    public String generateMarkdownDocs(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        List<Analysis> analyses = analysisRepository.findByProjectOrderByVersionDesc(project);
        Analysis latest = analyses.stream()
                .filter(a -> a.getStatus() == AnalysisStatus.COMPLETED)
                .findFirst()
                .orElse(null);

        if (latest == null) {
            return "# Documentation - " + project.getName() + "\n\nNo analysis run completed yet.";
        }

        List<ClassMetadata> classes = classMetadataRepository.findByAnalysis(latest);
        List<DependencyMetadata> dependencies = dependencyMetadataRepository.findByAnalysis(latest);
        List<MethodMetadata> methods = methodMetadataRepository.findByAnalysis(latest);

        String prompt = "You are a professional technical documentation writer.\n" +
                "Generate a detailed codebase documentation suite for the project: " + project.getName() + ".\n\n" +
                "You must strictly output four sections in clean Markdown:\n" +
                "### 1. Project Overview\n" +
                "Analyze the classes and namespaces. Provide an introduction to the project's purpose and its components.\n\n" +
                "### 2. Architecture Documentation\n" +
                "Describe the architectural patterns (e.g. MVC, Clean Architecture), how controllers, services, and repositories interact, and details about coupling density (" + dependencies.size() + " connections total).\n\n" +
                "### 3. API Documentation\n" +
                "List and document all REST endpoints. Cite the HTTP verb, route path, return type, and controller source class name.\n\n" +
                "### 4. Technical Documentation\n" +
                "Describe project build specifications, Flyway migrations, and code constraints. Include folders structure suggestions.\n\n" +
                "Here is the context of classes, endpoints, and dependencies:\n" +
                "- Classes: " + classes.size() + "\n" +
                "- Endpoints: " + methods.stream().filter(m -> m.getIsApiEndpoint() != null && m.getIsApiEndpoint()).count() + "\n" +
                "- Dependencies: " + dependencies.size() + "\n";

        try {
            ChatResponse chatResponse = chatModel.call(new Prompt(List.of(new SystemMessage(prompt), new UserMessage("Generate markdown documentation."))));
            return chatResponse.getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("Failed to generate documentation via AI, using fallback generator", e);
            return generateFallbackMarkdown(project, latest, classes, dependencies, methods);
        }
    }

    @Transactional(readOnly = true)
    public String generateHtmlDocs(Long projectId) {
        String md = generateMarkdownDocs(projectId);
        return convertMarkdownToHtml(md);
    }

    @Transactional(readOnly = true)
    public byte[] generatePdfDocs(Long projectId) {
        String md = generateMarkdownDocs(projectId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Project project = projectRepository.findById(projectId).orElse(null);
            document.add(new Paragraph("Codebase Documentation Report", titleFont));
            document.add(new Paragraph("Project: " + (project != null ? project.getName() : "Sample"), textFont));
            document.add(new Paragraph("Generated on: " + LocalDateTime.now().toString(), textFont));
            document.add(new Paragraph("\n"));

            String[] lines = md.split("\n");
            for (String line : lines) {
                if (line.startsWith("###") || line.startsWith("##") || line.startsWith("#")) {
                    document.add(new Paragraph("\n"));
                    document.add(new Paragraph(line.replaceAll("#+", "").trim(), sectionFont));
                } else if (!line.trim().isEmpty()) {
                    document.add(new Paragraph(line.trim(), textFont));
                }
            }

            document.close();
        } catch (Exception e) {
            log.error("Failed to generate PDF document", e);
        }

        return out.toByteArray();
    }

    private String generateFallbackMarkdown(Project project, Analysis analysis, List<ClassMetadata> classes, List<DependencyMetadata> dependencies, List<MethodMetadata> methods) {
        long eps = methods.stream().filter(m -> m.getIsApiEndpoint() != null && m.getIsApiEndpoint()).count();
        StringBuilder sb = new StringBuilder();
        sb.append("# Architectural Documentation Suite\n\n");
        sb.append("## 1. Project Overview\n");
        sb.append("This document outlines the software configuration for the project **").append(project.getName()).append("**.\n");
        sb.append("The repository contains a total of ").append(analysis.getTotalFiles()).append(" files, comprising ")
          .append(classes.size()).append(" structural classes and interfaces. The application provides clean component models.\n\n");

        sb.append("## 2. Architecture Documentation\n");
        sb.append("The codebase follows a modular MVC alignment. Dependencies flow from incoming controllers to core business services and database persistence engines.\n");
        sb.append("- Total Coupling Connections: ").append(dependencies.size()).append("\n");
        sb.append("- Dependencies mapping profile:\n");
        for (DependencyMetadata dm : dependencies) {
            sb.append("  * `").append(dm.getSourceNode()).append("` depends on `").append(dm.getTargetNode()).append("` [").append(dm.getType()).append("]\n");
        }
        sb.append("\n");

        sb.append("## 3. API Documentation\n");
        sb.append("Exposed REST API Endpoints Registry: (Total count: ").append(eps).append(")\n");
        for (MethodMetadata mm : methods) {
            if (mm.getIsApiEndpoint() != null && mm.getIsApiEndpoint()) {
                sb.append("- **").append(mm.getHttpMethod()).append("** `").append(mm.getPath()).append("` | Controller: `").append(mm.getClassMetadata().getName()).append("` | Handler: `").append(mm.getName()).append("()`\n");
            }
        }
        sb.append("\n");

        sb.append("## 4. Technical Documentation\n");
        sb.append("### Compilation Specs\n");
        sb.append("- Language: Java / JavaScript / Python\n");
        sb.append("- Persistence Layer: Spring Data JPA / MySQL\n");
        sb.append("- Migrations: Flyway Database Migrations configured.\n");

        return sb.toString();
    }

    private String convertMarkdownToHtml(String md) {
        String html = md
            .replaceAll("(?m)^# (.*)$", "<h1>$1</h1>")
            .replaceAll("(?m)^## (.*)$", "<h2>$1</h2>")
            .replaceAll("(?m)^### (.*)$", "<h3>$1</h3>")
            .replaceAll("(?m)^\\* (.*)$", "<li>$1</li>")
            .replaceAll("(?m)^- (.*)$", "<li>$1</li>")
            .replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>")
            .replaceAll("`(.*?)`", "<code>$1</code>")
            .replaceAll("\n", "<br/>");
        return "<div class='prose prose-invert max-w-none text-xs leading-relaxed text-gray-300'>" + html + "</div>";
    }
}
