package com.aisoftwarearchitect.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchitectureSummaryDto {
    private int totalClasses;
    private int totalServices;
    private int totalControllers;
    private int totalRepositories;
    private int totalEndpoints;
    private int dependencyCount;

    // Insights
    private String layeredArchitectureType;
    private boolean hasCircularDependencies;
    private List<List<String>> circularDependencies;
    private String mostConnectedComponent;
    private int mostConnectedComponentDegree;
    private String largestModule;
    private int largestModuleSize;
    private int healthScore;
    private String technicalDebt;
    private String telemetryReport;

    private List<String> healthScoreFactors;
    private int architectureScore;
    private String technicalDebtScore;
    private double couplingScore;
    private double complexityScore;
}
