package com.aisoftwarearchitect.core.service;

import com.aisoftwarearchitect.core.domain.*;
import com.aisoftwarearchitect.core.repository.*;
import com.aisoftwarearchitect.core.service.analysis.ArchitectureSummaryEngine;
import com.aisoftwarearchitect.core.service.parser.CodeParser;
import com.aisoftwarearchitect.core.service.parser.ParsedClass;
import com.aisoftwarearchitect.core.service.parser.ParsedMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisService {

    private final ProjectRepository projectRepository;
    private final AnalysisRepository analysisRepository;
    private final SourceFileRepository sourceFileRepository;
    private final ClassMetadataRepository classMetadataRepository;
    private final MethodMetadataRepository methodMetadataRepository;
    private final DependencyMetadataRepository dependencyMetadataRepository;
    private final CodeParser codeParser;
    private final ArchitectureSummaryEngine summaryEngine;
    private final ObjectMapper objectMapper;

    @Data
    @AllArgsConstructor
    public static class TempFile {
        private String filePath;
        private String content;
    }

    public Project createProject(String name, String description, String repoUrl, User owner) {
        String repoOwner = null;
        String repoName = null;

        if (repoUrl != null && !repoUrl.trim().isEmpty()) {
            // Basic parsing of github link: https://github.com/owner/name
            String cleanUrl = repoUrl.trim().replace("https://github.com/", "");
            String[] parts = cleanUrl.split("/");
            if (parts.length >= 2) {
                repoOwner = parts[0];
                repoName = parts[1].replace(".git", "");
            }
        }

        Project project = Project.builder()
                .name(name)
                .description(description)
                .repositoryUrl(repoUrl)
                .repoOwner(repoOwner)
                .repositoryName(repoName)
                .branchName("main")
                .owner(owner)
                .build();

        return projectRepository.save(project);
    }

    @Transactional
    public Analysis createAnalysis(Project project) {
        int nextVersion = analysisRepository.findMaxVersionByProject(project) + 1;
        Analysis analysis = Analysis.builder()
                .project(project)
                .version(nextVersion)
                .status(AnalysisStatus.PENDING)
                .totalFiles(0)
                .totalClasses(0)
                .totalInterfaces(0)
                .totalServices(0)
                .totalControllers(0)
                .totalEndpoints(0)
                .build();
        return analysisRepository.save(analysis);
    }

    @Async
    @Transactional
    public void executeAnalysisAsync(Long analysisId, List<TempFile> tempFiles) {
        log.info("Starting asynchronous code analysis job for Analysis ID: {}", analysisId);
        Optional<Analysis> analysisOpt = analysisRepository.findById(analysisId);
        if (analysisOpt.isEmpty()) {
            log.error("Analysis record not found: {}", analysisId);
            return;
        }

        Analysis analysis = analysisOpt.get();
        analysis.setStatus(AnalysisStatus.RUNNING);
        analysisRepository.save(analysis);

        try {
            int totalFiles = 0;
            int totalClasses = 0;
            int totalInterfaces = 0;
            int totalServices = 0;
            int totalControllers = 0;
            int totalEndpoints = 0;

            List<SourceFile> savedSourceFiles = new ArrayList<>();
            List<ClassMetadata> savedClasses = new ArrayList<>();
            List<DependencyMetadata> savedDependencies = new ArrayList<>();

            // 1. Process files and parse classes
            Map<String, List<ParsedClass>> fileToClassesMap = new HashMap<>();

            for (TempFile tempFile : tempFiles) {
                String ext = getFileExtension(tempFile.getFilePath()).toUpperCase();
                if (!isSupportedExtension(ext)) {
                    continue;
                }

                totalFiles++;
                
                // Save SourceFile
                SourceFile sf = SourceFile.builder()
                        .analysis(analysis)
                        .filePath(tempFile.getFilePath())
                        .fileName(getFileName(tempFile.getFilePath()))
                        .fileType(ext)
                        .content(tempFile.getContent())
                        .build();
                sf = sourceFileRepository.save(sf);
                savedSourceFiles.add(sf);

                // Parse classes in file
                List<ParsedClass> parsedClasses = codeParser.parseFile(tempFile.getContent(), ext);
                if (!parsedClasses.isEmpty()) {
                    fileToClassesMap.put(tempFile.getFilePath(), parsedClasses);
                    
                    for (ParsedClass pc : parsedClasses) {
                        totalClasses++;
                        if ("INTERFACE".equalsIgnoreCase(pc.getType())) totalInterfaces++;
                        if ("SERVICE".equalsIgnoreCase(pc.getStereotype())) totalServices++;
                        if ("CONTROLLER".equalsIgnoreCase(pc.getStereotype())) totalControllers++;

                        // Save ClassMetadata
                        ClassMetadata cm = ClassMetadata.builder()
                                .sourceFile(sf)
                                .name(pc.getName())
                                .type(pc.getType())
                                .stereotype(pc.getStereotype())
                                .packageName(pc.getPackageName())
                                .build();
                        cm = classMetadataRepository.save(cm);
                        savedClasses.add(cm);

                        // Save Methods
                        for (ParsedMethod pm : pc.getMethods()) {
                            if (pm.isApiEndpoint()) totalEndpoints++;

                            MethodMetadata mm = MethodMetadata.builder()
                                    .classMetadata(cm)
                                    .name(pm.getName())
                                    .returnType(pm.getReturnType())
                                    .isApiEndpoint(pm.isApiEndpoint())
                                    .httpMethod(pm.getHttpMethod())
                                    .path(pm.getPath())
                                    .build();
                            methodMetadataRepository.save(mm);
                        }
                    }
                }
            }

            // 2. Resolve dependencies between classes
            Set<String> classNamesInProject = savedClasses.stream()
                    .map(ClassMetadata::getName)
                    .collect(Collectors.toSet());

            for (Map.Entry<String, List<ParsedClass>> entry : fileToClassesMap.entrySet()) {
                for (ParsedClass pc : entry.getValue()) {
                    for (String imported : pc.getDependencies()) {
                        if (classNamesInProject.contains(imported) && !imported.equals(pc.getName())) {
                            DependencyMetadata dm = DependencyMetadata.builder()
                                    .analysis(analysis)
                                    .sourceNode(pc.getName())
                                    .targetNode(imported)
                                    .type("DEPENDS_ON")
                                    .build();
                            dm = dependencyMetadataRepository.save(dm);
                            savedDependencies.add(dm);
                        }
                    }
                }
            }

            // 3. Generate Architecture summary
            String summaryText = summaryEngine.generateSummary(savedClasses, savedDependencies);

            // 4. Save JSON document output
            Map<String, Object> analysisOutput = new HashMap<>();
            analysisOutput.put("summary", summaryText);
            analysisOutput.put("metrics", Map.of(
                "totalFiles", totalFiles,
                "totalClasses", totalClasses,
                "totalInterfaces", totalInterfaces,
                "totalServices", totalServices,
                "totalControllers", totalControllers,
                "totalEndpoints", totalEndpoints
            ));
            
            List<Map<String, Object>> classList = new ArrayList<>();
            for (ClassMetadata cm : savedClasses) {
                classList.add(Map.of(
                    "name", cm.getName(),
                    "type", cm.getType(),
                    "stereotype", cm.getStereotype(),
                    "package", cm.getPackageName() == null ? "" : cm.getPackageName()
                ));
            }
            analysisOutput.put("classes", classList);

            String jsonOutput = objectMapper.writeValueAsString(analysisOutput);

            // 5. Complete Analysis
            analysis.setStatus(AnalysisStatus.COMPLETED);
            analysis.setCompletedAt(LocalDateTime.now());
            analysis.setTotalFiles(totalFiles);
            analysis.setTotalClasses(totalClasses);
            analysis.setTotalInterfaces(totalInterfaces);
            analysis.setTotalServices(totalServices);
            analysis.setTotalControllers(totalControllers);
            analysis.setTotalEndpoints(totalEndpoints);
            analysis.setAnalysisJson(jsonOutput);
            analysis.setArchitectureSummary(summaryText);
            
            analysisRepository.save(analysis);
            log.info("Analysis ID: {} completed successfully.", analysisId);

        } catch (Exception e) {
            log.error("Error running async analysis", e);
            analysis.setStatus(AnalysisStatus.FAILED);
            analysis.setCompletedAt(LocalDateTime.now());
            analysis.setArchitectureSummary("# Analysis Failed\n\nAn error occurred during scanning: " + e.getMessage());
            analysisRepository.save(analysis);
        }
    }

    public List<TempFile> extractZipFiles(InputStream is) throws IOException {
        List<TempFile> files = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            byte[] buffer = new byte[4096];
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String path = entry.getName();
                if (isIgnoredPath(path)) {
                    continue;
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }

                String content = baos.toString("UTF-8");
                files.add(new TempFile(path, content));
            }
        }
        return files;
    }

    public InputStream downloadGitHubZip(String repoUrl) throws IOException {
        String cleanUrl = repoUrl.trim();
        if (cleanUrl.endsWith("/")) {
            cleanUrl = cleanUrl.substring(0, cleanUrl.length() - 1);
        }
        
        // Remove .git if appended
        if (cleanUrl.endsWith(".git")) {
            cleanUrl = cleanUrl.substring(0, cleanUrl.length() - 4);
        }
        
        String zipUrl = cleanUrl + "/archive/refs/heads/main.zip";
        log.info("Downloading ZIP from GitHub URL: {}", zipUrl);
        
        URL url = new URL(zipUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        
        int status = conn.getResponseCode();
        if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM) {
            String redirectUrl = conn.getHeaderField("Location");
            log.info("Redirected to: {}", redirectUrl);
            url = new URL(redirectUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        } else if (status != HttpURLConnection.HTTP_OK) {
            // Try fallback to master branch
            String fallbackUrl = cleanUrl + "/archive/refs/heads/master.zip";
            log.info("Main branch download failed, trying Master: {}", fallbackUrl);
            url = new URL(fallbackUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        }
        
        return conn.getInputStream();
    }

    private boolean isIgnoredPath(String path) {
        String lower = path.toLowerCase();
        return lower.contains("node_modules/") ||
               lower.contains(".git/") ||
               lower.contains("target/") ||
               lower.contains("build/") ||
               lower.contains(".idea/") ||
               lower.contains(".vscode/") ||
               lower.contains("venv/") ||
               lower.contains("__pycache__/");
    }

    private boolean isSupportedExtension(String ext) {
        return "JAVA".equals(ext) || "PY".equals(ext) || "JS".equals(ext) || "TS".equals(ext) || "TSX".equals(ext) || "JSX".equals(ext);
    }

    private String getFileExtension(String path) {
        int idx = path.lastIndexOf('.');
        return idx > 0 ? path.substring(idx + 1) : "";
    }

    private String getFileName(String path) {
        int idx = path.lastIndexOf('/');
        return idx > 0 ? path.substring(idx + 1) : path;
    }
}
