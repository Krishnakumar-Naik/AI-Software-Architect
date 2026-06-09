package com.aisoftwarearchitect.presentation.controller;

import com.aisoftwarearchitect.core.domain.*;
import com.aisoftwarearchitect.core.repository.*;
import com.aisoftwarearchitect.core.service.GraphGenerationService;
import com.aisoftwarearchitect.core.service.DocumentationService;
import com.aisoftwarearchitect.core.service.EngineeringReportService;
import com.aisoftwarearchitect.core.service.analysis.ArchitectureInsightEngine;
import com.aisoftwarearchitect.presentation.dto.ArchitectureSummaryDto;
import com.aisoftwarearchitect.presentation.dto.GraphDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Slf4j
public class PublicShareController {

    private final ProjectRepository projectRepository;
    private final AnalysisRepository analysisRepository;
    private final ClassMetadataRepository classMetadataRepository;
    private final DependencyMetadataRepository dependencyMetadataRepository;
    private final MethodMetadataRepository methodMetadataRepository;
    private final GraphGenerationService graphGenerationService;
    private final ArchitectureInsightEngine insightEngine;
    private final DocumentationService documentationService;
    private final EngineeringReportService reportService;

    @GetMapping("/projects/{projectId}/graph")
    public ResponseEntity<?> getPublicGraph(@PathVariable Long projectId) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            if (projectId == 1L) {
                return ResponseEntity.ok(generateMockGraph());
            }
            return ResponseEntity.notFound().build();
        }

        if (project.getIsPublic() == null || !project.getIsPublic()) {
            return ResponseEntity.status(403).body("This project is not publicly shared.");
        }

        Analysis latestCompleted = getLatestCompletedAnalysis(project);
        if (latestCompleted == null) {
            return ResponseEntity.badRequest().body("No completed analysis run found.");
        }

        GraphDto graph = graphGenerationService.generateGraph(latestCompleted);
        return ResponseEntity.ok(graph);
    }

    @GetMapping("/projects/{projectId}/architecture-summary")
    public ResponseEntity<?> getPublicSummary(@PathVariable Long projectId) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            if (projectId == 1L) {
                return ResponseEntity.ok(generateMockSummary());
            }
            return ResponseEntity.notFound().build();
        }

        if (project.getIsPublic() == null || !project.getIsPublic()) {
            return ResponseEntity.status(403).body("This project is not publicly shared.");
        }

        Analysis latestCompleted = getLatestCompletedAnalysis(project);
        if (latestCompleted == null) {
            return ResponseEntity.badRequest().body("No completed codebase scan run found.");
        }

        List<ClassMetadata> classes = classMetadataRepository.findByAnalysis(latestCompleted);
        List<DependencyMetadata> dependencies = dependencyMetadataRepository.findByAnalysis(latestCompleted);
        List<MethodMetadata> methods = methodMetadataRepository.findByAnalysis(latestCompleted);

        ArchitectureInsightEngine.Insights insights = insightEngine.calculateInsights(classes, dependencies, methods);
        int totalRepositories = (int) classes.stream().filter(c -> "REPOSITORY".equalsIgnoreCase(c.getStereotype())).count();

        ArchitectureSummaryDto dto = ArchitectureSummaryDto.builder()
                .totalClasses(latestCompleted.getTotalClasses())
                .totalServices(latestCompleted.getTotalServices())
                .totalControllers(latestCompleted.getTotalControllers())
                .totalRepositories(totalRepositories)
                .totalEndpoints(latestCompleted.getTotalEndpoints())
                .dependencyCount(dependencies.size())
                .layeredArchitectureType(insights.layeredArchitectureType)
                .hasCircularDependencies(!insights.circularDependencies.isEmpty())
                .circularDependencies(insights.circularDependencies)
                .mostConnectedComponent(insights.mostConnectedComponent)
                .mostConnectedComponentDegree(insights.mostConnectedComponentDegree)
                .largestModule(insights.largestModule)
                .largestModuleSize(insights.largestModuleSize)
                .healthScore(insights.healthScore)
                .technicalDebt(insights.technicalDebt)
                .telemetryReport(latestCompleted.getArchitectureSummary())
                .healthScoreFactors(insights.healthScoreFactors)
                .architectureScore(insights.architectureScore)
                .technicalDebtScore(insights.technicalDebtScore)
                .couplingScore(insights.couplingScore)
                .complexityScore(insights.complexityScore)
                .build();

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/projects/{projectId}/dependencies")
    public ResponseEntity<?> getPublicDependencies(@PathVariable Long projectId) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            if (projectId == 1L) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            return ResponseEntity.notFound().build();
        }

        if (project.getIsPublic() == null || !project.getIsPublic()) {
            return ResponseEntity.status(403).body("This project is not publicly shared.");
        }

        Analysis latestCompleted = getLatestCompletedAnalysis(project);
        if (latestCompleted == null) {
            return ResponseEntity.badRequest().body("No completed analysis run found.");
        }

        List<DependencyMetadata> dependencies = dependencyMetadataRepository.findByAnalysis(latestCompleted);
        return ResponseEntity.ok(dependencies);
    }

    @GetMapping("/projects/{projectId}/documentation")
    public ResponseEntity<?> getPublicDocumentation(@PathVariable Long projectId) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            if (projectId == 1L) {
                return ResponseEntity.ok(Map.of("markdown", generateMockDocumentation()));
            }
            return ResponseEntity.notFound().build();
        }

        if (project.getIsPublic() == null || !project.getIsPublic()) {
            return ResponseEntity.status(403).body("This project is not publicly shared.");
        }

        String markdown = documentationService.generateMarkdownDocs(projectId);
        return ResponseEntity.ok(Map.of("markdown", markdown));
    }

    @GetMapping("/projects/{projectId}/reports")
    public ResponseEntity<?> getPublicReports(@PathVariable Long projectId) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            if (projectId == 1L) {
                return ResponseEntity.ok(generateMockReports());
            }
            return ResponseEntity.notFound().build();
        }

        if (project.getIsPublic() == null || !project.getIsPublic()) {
            return ResponseEntity.status(403).body("This project is not publicly shared.");
        }

        Map<String, String> reports = reportService.generateReports(projectId);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/projects/{projectId}/documentation/pdf")
    public ResponseEntity<byte[]> getPublicPdfDocs(@PathVariable Long projectId) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project != null && (project.getIsPublic() == null || !project.getIsPublic())) {
            return ResponseEntity.status(403).body(null);
        }

        byte[] pdfBytes = documentationService.generatePdfDocs(projectId);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "codebase-documentation.pdf");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @GetMapping("/projects/{projectId}/reports/pdf")
    public ResponseEntity<byte[]> getPublicPdfReport(@PathVariable Long projectId) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project != null && (project.getIsPublic() == null || !project.getIsPublic())) {
            return ResponseEntity.status(403).body(null);
        }

        byte[] pdfBytes = reportService.generatePdfReport(projectId);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "engineering-architecture-report.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    private Analysis getLatestCompletedAnalysis(Project project) {
        List<Analysis> runs = analysisRepository.findByProjectOrderByVersionDesc(project);
        return runs.stream()
                .filter(a -> a.getStatus() == AnalysisStatus.COMPLETED)
                .findFirst()
                .orElse(null);
    }

    // High fidelity mock generator for showcase fallbacks
    private GraphDto generateMockGraph() {
        List<GraphDto.NodeDataWrapper> nodes = new ArrayList<>();
        List<GraphDto.EdgeDataWrapper> edges = new ArrayList<>();

        nodes.add(createMockNode("UserController", "CONTROLLER"));
        nodes.add(createMockNode("UserService", "SERVICE"));
        nodes.add(createMockNode("UserRepository", "REPOSITORY"));
        nodes.add(createMockNode("User", "CLASS"));
        nodes.add(createMockNode("[GET]/api/users", "ENDPOINT"));

        edges.add(createMockEdge("UserController", "UserService", "CALLS"));
        edges.add(createMockEdge("UserService", "UserRepository", "CALLS"));
        edges.add(createMockEdge("UserRepository", "User", "USES"));
        edges.add(createMockEdge("UserController", "[GET]/api/users", "EXPOSES_ENDPOINT"));

        return GraphDto.builder().nodes(nodes).edges(edges).build();
    }

    private GraphDto.NodeDataWrapper createMockNode(String id, String type) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("stereotype", type);
        meta.put("degree", 2);
        return GraphDto.NodeDataWrapper.builder()
                .data(GraphDto.NodeData.builder()
                        .id(id)
                        .label(id)
                        .type(type)
                        .metadata(meta)
                        .build())
                .build();
    }

    private GraphDto.EdgeDataWrapper createMockEdge(String src, String tgt, String rel) {
        return GraphDto.EdgeDataWrapper.builder()
                .data(GraphDto.EdgeData.builder()
                        .id("e-" + src + "-" + tgt)
                        .source(src)
                        .target(tgt)
                        .relationshipType(rel)
                        .build())
                .build();
    }

    private ArchitectureSummaryDto generateMockSummary() {
        return ArchitectureSummaryDto.builder()
                .totalClasses(15)
                .totalServices(4)
                .totalControllers(2)
                .totalRepositories(2)
                .totalEndpoints(4)
                .dependencyCount(12)
                .layeredArchitectureType("Strict Clean Architecture")
                .hasCircularDependencies(false)
                .circularDependencies(new ArrayList<>())
                .mostConnectedComponent("UserService")
                .mostConnectedComponentDegree(3)
                .largestModule("com.aisoftwarearchitect.core")
                .largestModuleSize(8)
                .healthScore(96)
                .technicalDebt("LOW")
                .telemetryReport("No violations found. Coupling is optimal.")
                .healthScoreFactors(Arrays.asList(
                        "Circular Dependencies: No cycles detected (+0)",
                        "Layer Violations: Clean layers (+0)",
                        "Coupling: Optimal coupling density (+0)",
                        "Package Balance: Balanced modular layout (+0)",
                        "Endpoint Complexity: Low endpoint density (+0)"
                ))
                .architectureScore(96)
                .technicalDebtScore("LOW")
                .couplingScore(0.8)
                .complexityScore(2.0)
                .build();
    }

    private String generateMockDocumentation() {
        return "# AI Software Architect Sample Project\n\n" +
                "## 1. Project Overview\n" +
                "This sample project displays an automated layout parsing showcase representing a clean Spring Boot MVC REST API.\n\n" +
                "## 2. Architecture Documentation\n" +
                "Follows strict MVC boundaries. Controllers map REST inputs, delegating database reads to JPA repository layouts.\n\n" +
                "## 3. API Documentation\n" +
                "- **GET** `/api/users` | Controller: `UserController` | Return: List<User>\n\n" +
                "## 4. Technical Documentation\n" +
                "Java 21, Spring Boot 3, and Spring Data JPA.";
    }

    private Map<String, String> generateMockReports() {
        Map<String, String> reports = new HashMap<>();
        reports.put("architecture", "### Architecture Report\n\nClean boundaries are verified. High cohesion detected.");
        reports.put("techDebt", "### Technical Debt Report\n\nLow code debt. No circular dependencies present.");
        reports.put("dependency", "### Dependency Report\n\nNo cycles. Direct DAG mapping confirmed.");
        reports.put("security", "### Security Report\n\nSecurity configuration permitted public routes cleanly.");
        return reports;
    }
}
