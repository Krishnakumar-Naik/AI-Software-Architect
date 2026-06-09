package com.aisoftwarearchitect.core.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Analysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalysisStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "total_files")
    @Builder.Default
    private Integer totalFiles = 0;

    @Column(name = "total_classes")
    @Builder.Default
    private Integer totalClasses = 0;

    @Column(name = "total_interfaces")
    @Builder.Default
    private Integer totalInterfaces = 0;

    @Column(name = "total_services")
    @Builder.Default
    private Integer totalServices = 0;

    @Column(name = "total_controllers")
    @Builder.Default
    private Integer totalControllers = 0;

    @Column(name = "total_endpoints")
    @Builder.Default
    private Integer totalEndpoints = 0;

    @Column(name = "analysis_json", columnDefinition = "LONGTEXT")
    private String analysisJson;

    @Column(name = "architecture_summary", columnDefinition = "LONGTEXT")
    private String architectureSummary;

    @PrePersist
    protected void onStart() {
        startedAt = LocalDateTime.now();
    }
}
