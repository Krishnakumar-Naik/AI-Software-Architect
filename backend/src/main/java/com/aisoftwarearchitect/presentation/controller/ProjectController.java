package com.aisoftwarearchitect.presentation.controller;

import com.aisoftwarearchitect.core.domain.Analysis;
import com.aisoftwarearchitect.core.domain.Project;
import com.aisoftwarearchitect.core.domain.User;
import com.aisoftwarearchitect.core.service.AnalysisService;
import com.aisoftwarearchitect.core.service.UserService;
import com.aisoftwarearchitect.presentation.dto.GitHubProjectRequest;
import com.aisoftwarearchitect.presentation.dto.PasteProjectRequest;
import com.aisoftwarearchitect.presentation.dto.ProjectDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final UserService userService;
    private final AnalysisService analysisService;
    private final com.aisoftwarearchitect.core.repository.ProjectRepository projectRepository;

    @GetMapping
    public ResponseEntity<List<ProjectDto>> getProjects(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        User owner = userService.getUserByUsername(principal.getName());
        List<Project> projects = projectRepository.findByOwner(owner);
        
        List<ProjectDto> dtos = projects.stream()
                .map(p -> ProjectDto.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .description(p.getDescription())
                        .repositoryUrl(p.getRepositoryUrl())
                        .repoOwner(p.getRepoOwner())
                        .repositoryName(p.getRepositoryName())
                        .branchName(p.getBranchName())
                        .isPublic(p.getIsPublic())
                        .createdAt(p.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{projectId}/toggle-share")
    public ResponseEntity<?> toggleShare(Principal principal, @PathVariable Long projectId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        User owner = userService.getUserByUsername(principal.getName());
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }
        if (!project.getOwner().getId().equals(owner.getId())) {
            return ResponseEntity.status(403).body("Access Denied");
        }

        project.setIsPublic(project.getIsPublic() == null ? true : !project.getIsPublic());
        projectRepository.save(project);

        return ResponseEntity.ok(Map.of(
                "projectId", project.getId(),
                "isPublic", project.getIsPublic()
        ));
    }

    @PostMapping("/upload-zip")
    public ResponseEntity<?> uploadZip(Principal principal,
                                       @RequestParam("name") String name,
                                       @RequestParam(value = "description", required = false) String description,
                                       @RequestParam("file") MultipartFile file) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        User owner = userService.getUserByUsername(principal.getName());
        log.info("Request to upload ZIP project: {} by user {}", name, owner.getUsername());

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("ZIP file is empty");
            }

            List<AnalysisService.TempFile> tempFiles;
            try (InputStream is = file.getInputStream()) {
                tempFiles = analysisService.extractZipFiles(is);
            }

            Project project = analysisService.createProject(name, description, null, owner);
            Analysis analysis = analysisService.createAnalysis(project);
            
            analysisService.executeAnalysisAsync(analysis.getId(), tempFiles);

            return ResponseEntity.ok(Map.of(
                    "project", mapProjectToDto(project),
                    "analysisId", analysis.getId(),
                    "status", analysis.getStatus().name()
            ));

        } catch (Exception e) {
            log.error("Failed to upload ZIP", e);
            return ResponseEntity.internalServerError().body("Failed to process ZIP file: " + e.getMessage());
        }
    }

    @PostMapping("/paste")
    public ResponseEntity<?> pasteCode(Principal principal,
                                       @Valid @RequestBody PasteProjectRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        User owner = userService.getUserByUsername(principal.getName());
        log.info("Request to paste code project: {} by user {}", request.getName(), owner.getUsername());

        try {
            Project project = analysisService.createProject(request.getName(), request.getDescription(), null, owner);
            Analysis analysis = analysisService.createAnalysis(project);

            List<AnalysisService.TempFile> tempFiles = new ArrayList<>();
            tempFiles.add(new AnalysisService.TempFile(request.getFileName(), request.getCodeContent()));

            analysisService.executeAnalysisAsync(analysis.getId(), tempFiles);

            return ResponseEntity.ok(Map.of(
                    "project", mapProjectToDto(project),
                    "analysisId", analysis.getId(),
                    "status", analysis.getStatus().name()
            ));
        } catch (Exception e) {
            log.error("Failed to save pasted code", e);
            return ResponseEntity.internalServerError().body("Failed to process pasted code: " + e.getMessage());
        }
    }

    @PostMapping("/github")
    public ResponseEntity<?> githubImport(Principal principal,
                                          @Valid @RequestBody GitHubProjectRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        User owner = userService.getUserByUsername(principal.getName());
        log.info("Request to import GitHub repository: {} by user {}", request.getCloneUrl(), owner.getUsername());

        try {
            Project project = analysisService.createProject(request.getName(), request.getDescription(), request.getCloneUrl(), owner);
            Analysis analysis = analysisService.createAnalysis(project);

            List<AnalysisService.TempFile> tempFiles;
            try (InputStream is = analysisService.downloadGitHubZip(request.getCloneUrl())) {
                tempFiles = analysisService.extractZipFiles(is);
            }

            analysisService.executeAnalysisAsync(analysis.getId(), tempFiles);

            return ResponseEntity.ok(Map.of(
                    "project", mapProjectToDto(project),
                    "analysisId", analysis.getId(),
                    "status", analysis.getStatus().name()
            ));
        } catch (Exception e) {
            log.error("Failed to import GitHub repository", e);
            return ResponseEntity.internalServerError().body("Failed to process GitHub repository: " + e.getMessage());
        }
    }

    private ProjectDto mapProjectToDto(Project p) {
        return ProjectDto.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .repositoryUrl(p.getRepositoryUrl())
                .repoOwner(p.getRepoOwner())
                .repositoryName(p.getRepositoryName())
                .branchName(p.getBranchName())
                .isPublic(p.getIsPublic())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
