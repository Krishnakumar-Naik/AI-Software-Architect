package com.aisoftwarearchitect.presentation.controller;

import com.aisoftwarearchitect.core.service.DocumentationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/documentation")
@RequiredArgsConstructor
@Slf4j
public class DocumentationController {

    private final DocumentationService documentationService;

    @PostMapping("/generate")
    public ResponseEntity<?> generateDocs(Principal principal, 
                                          @RequestParam("projectId") Long projectId,
                                          @RequestParam(value = "format", defaultValue = "markdown") String format) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        log.info("Generating documentation in {} for project ID: {}", format, projectId);
        if ("html".equalsIgnoreCase(format)) {
            String html = documentationService.generateHtmlDocs(projectId);
            return ResponseEntity.ok(Map.of("html", html));
        } else {
            String markdown = documentationService.generateMarkdownDocs(projectId);
            return ResponseEntity.ok(Map.of("markdown", markdown));
        }
    }

    @GetMapping("/projects/{projectId}/pdf")
    public ResponseEntity<byte[]> getPdfDocs(Principal principal, @PathVariable Long projectId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        byte[] pdfBytes = documentationService.generatePdfDocs(projectId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "codebase-documentation.pdf");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
