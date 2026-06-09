package com.aisoftwarearchitect.presentation.controller;

import com.aisoftwarearchitect.core.service.EngineeringReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final EngineeringReportService reportService;

    @GetMapping
    public ResponseEntity<?> getReports(Principal principal, @PathVariable Long projectId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        log.info("Fetching engineering reports for project ID: {}", projectId);
        Map<String, String> reports = reportService.generateReports(projectId);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> getPdfReport(Principal principal, @PathVariable Long projectId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        log.info("Streaming engineering reports PDF for project ID: {}", projectId);
        byte[] pdfBytes = reportService.generatePdfReport(projectId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "engineering-architecture-report.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
