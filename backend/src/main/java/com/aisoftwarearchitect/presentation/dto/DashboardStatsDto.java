package com.aisoftwarearchitect.presentation.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDto {
    private int projectsAnalyzed;
    private int architectureReports;
    private int documentationGenerated;
    private int aiConversations;
    
    private List<RecentActivity> recentActivities;
    private List<SystemHealth> systemHealth;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentActivity {
        private String id;
        private String project;
        private String action;
        private String time;
        private String status; // SUCCESS, IN_PROGRESS, FAILED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SystemHealth {
        private String service;
        private String status; // UP, DOWN
        private double latencyMs;
    }
}
