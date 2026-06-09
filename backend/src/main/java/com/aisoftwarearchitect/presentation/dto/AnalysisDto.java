package com.aisoftwarearchitect.presentation.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisDto {
    private Long id;
    private Long projectId;
    private Integer version;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer totalFiles;
    private Integer totalClasses;
    private Integer totalInterfaces;
    private Integer totalServices;
    private Integer totalControllers;
    private Integer totalEndpoints;
    private String analysisJson;
    private String architectureSummary;
}
