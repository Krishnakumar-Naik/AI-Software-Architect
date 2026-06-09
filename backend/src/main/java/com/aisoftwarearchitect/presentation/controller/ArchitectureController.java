package com.aisoftwarearchitect.presentation.controller;

import com.aisoftwarearchitect.core.domain.*;
import com.aisoftwarearchitect.core.repository.*;
import com.aisoftwarearchitect.core.service.GraphGenerationService;
import com.aisoftwarearchitect.core.service.analysis.ArchitectureInsightEngine;
import com.aisoftwarearchitect.presentation.dto.ArchitectureSummaryDto;
import com.aisoftwarearchitect.presentation.dto.GraphDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
@Slf4j
public class ArchitectureController {

    private final ProjectRepository projectRepository;
    private final AnalysisRepository analysisRepository;
    private final ClassMetadataRepository classMetadataRepository;
    private final MethodMetadataRepository methodMetadataRepository;
    private final DependencyMetadataRepository dependencyMetadataRepository;
    private final GraphGenerationService graphGenerationService;
    private final ArchitectureInsightEngine insightEngine;

    @GetMapping("/graph")
    public ResponseEntity<?> getProjectGraph(Principal principal, @PathVariable Long projectId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }

        Analysis latestCompleted = getLatestCompletedAnalysis(project);
        if (latestCompleted == null) {
            return ResponseEntity.badRequest().body("No completed codebase scan run found. Please execute analysis first.");
        }

        GraphDto graph = graphGenerationService.generateGraph(latestCompleted);
        return ResponseEntity.ok(graph);
    }

    @GetMapping("/architecture-summary")
    public ResponseEntity<?> getArchitectureSummary(Principal principal, @PathVariable Long projectId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }

        Analysis latestCompleted = getLatestCompletedAnalysis(project);
        if (latestCompleted == null) {
            return ResponseEntity.badRequest().body("No completed codebase scan run found. Please execute analysis first.");
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

    @GetMapping("/dependencies")
    public ResponseEntity<?> getProjectDependencies(Principal principal, @PathVariable Long projectId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }

        Analysis latestCompleted = getLatestCompletedAnalysis(project);
        if (latestCompleted == null) {
            return ResponseEntity.badRequest().body("No completed codebase scan run found. Please execute analysis first.");
        }

        List<DependencyMetadata> dependencies = dependencyMetadataRepository.findByAnalysis(latestCompleted);
        return ResponseEntity.ok(dependencies);
    }

    private Analysis getLatestCompletedAnalysis(Project project) {
        List<Analysis> runs = analysisRepository.findByProjectOrderByVersionDesc(project);
        return runs.stream()
                .filter(a -> a.getStatus() == AnalysisStatus.COMPLETED)
                .findFirst()
                .orElse(null);
    }
}
