package com.aisoftwarearchitect.core.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dependency_metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DependencyMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Column(name = "source_node", nullable = false)
    private String sourceNode;

    @Column(name = "target_node", nullable = false)
    private String targetNode;

    @Column(nullable = false, length = 20)
    private String type; // DEPENDS_ON, EXTENDS, IMPLEMENTS
}
