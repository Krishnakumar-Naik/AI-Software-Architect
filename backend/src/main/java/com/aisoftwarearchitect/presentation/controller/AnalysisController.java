package com.aisoftwarearchitect.presentation.controller;

import com.aisoftwarearchitect.core.domain.Analysis;
import com.aisoftwarearchitect.core.domain.Project;
import com.aisoftwarearchitect.core.domain.SourceFile;
import com.aisoftwarearchitect.core.repository.AnalysisRepository;
import com.aisoftwarearchitect.core.repository.ProjectRepository;
import com.aisoftwarearchitect.core.repository.SourceFileRepository;
import com.aisoftwarearchitect.core.service.AnalysisService;
import com.aisoftwarearchitect.presentation.dto.AnalysisDto;
import com.aisoftwarearchitect.presentation.dto.MessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AnalysisController {

    private final ProjectRepository projectRepository;
    private final AnalysisRepository analysisRepository;
    private final SourceFileRepository sourceFileRepository;
    private final AnalysisService analysisService;

    @GetMapping("/projects/{projectId}/analyses")
    public ResponseEntity<?> getProjectAnalyses(Principal principal, @PathVariable Long projectId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        List<Analysis> runs = analysisRepository.findByProjectOrderByVersionDesc(projectOpt.get());
        List<AnalysisDto> dtos = runs.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/analyses/{projectId}/start")
    public ResponseEntity<?> triggerNewAnalysis(Principal principal, @PathVariable Long projectId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Project project = projectOpt.get();
        if (project.getRepositoryUrl() == null || project.getRepositoryUrl().isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Only GitHub-linked projects can trigger standard re-analyses. For local projects, please upload a new code payload."));
        }

        try {
            log.info("Triggering new versioned analysis for GitHub project: {}", project.getName());
            
            Analysis analysis = analysisService.createAnalysis(project);
            
            List<AnalysisService.TempFile> tempFiles;
            try (InputStream is = analysisService.downloadGitHubZip(project.getRepositoryUrl())) {
                tempFiles = analysisService.extractZipFiles(is);
            }
            
            analysisService.executeAnalysisAsync(analysis.getId(), tempFiles);

            return ResponseEntity.ok(Map.of(
                    "analysisId", analysis.getId(),
                    "version", analysis.getVersion(),
                    "status", analysis.getStatus().name()
            ));
        } catch (Exception e) {
            log.error("Failed to run new analysis version", e);
            return ResponseEntity.internalServerError().body("Failed to run new analysis: " + e.getMessage());
        }
    }

    @GetMapping("/analyses/{analysisId}/results")
    public ResponseEntity<?> getAnalysisResults(Principal principal, @PathVariable Long analysisId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Optional<Analysis> analysisOpt = analysisRepository.findById(analysisId);
        if (analysisOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapToDto(analysisOpt.get()));
    }

    @GetMapping("/analyses/{analysisId}/files")
    public ResponseEntity<?> getAnalysisFiles(Principal principal, @PathVariable Long analysisId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Optional<Analysis> analysisOpt = analysisRepository.findById(analysisId);
        if (analysisOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<SourceFile> files = sourceFileRepository.findByAnalysis(analysisOpt.get());
        List<Map<String, Object>> filesMeta = files.stream()
                .map(f -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", f.getId());
                    m.put("filePath", f.getFilePath());
                    m.put("fileName", f.getFileName());
                    m.put("fileType", f.getFileType());
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(filesMeta);
    }

    @GetMapping("/analyses/files/{fileId}")
    public ResponseEntity<?> getFileContent(Principal principal, @PathVariable Long fileId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Optional<SourceFile> fileOpt = sourceFileRepository.findById(fileId);
        if (fileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        SourceFile f = fileOpt.get();
        Map<String, Object> res = new HashMap<>();
        res.put("id", f.getId());
        res.put("filePath", f.getFilePath());
        res.put("fileName", f.getFileName());
        res.put("fileType", f.getFileType());
        res.put("content", f.getContent());
        return ResponseEntity.ok(res);
    }

    private AnalysisDto mapToDto(Analysis a) {
        return AnalysisDto.builder()
                .id(a.getId())
                .projectId(a.getProject().getId())
                .version(a.getVersion())
                .status(a.getStatus().name())
                .startedAt(a.getStartedAt())
                .completedAt(a.getCompletedAt())
                .totalFiles(a.getTotalFiles())
                .totalClasses(a.getTotalClasses())
                .totalInterfaces(a.getTotalInterfaces())
                .totalServices(a.getTotalServices())
                .totalControllers(a.getTotalControllers())
                .totalEndpoints(a.getTotalEndpoints())
                .analysisJson(a.getAnalysisJson())
                .architectureSummary(a.getArchitectureSummary())
                .build();
    }
}
