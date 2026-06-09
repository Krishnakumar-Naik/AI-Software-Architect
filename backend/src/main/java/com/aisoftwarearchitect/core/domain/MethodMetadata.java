package com.aisoftwarearchitect.core.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "method_metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MethodMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_metadata_id", nullable = false)
    private ClassMetadata classMetadata;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "return_type", length = 100)
    private String returnType;

    @Column(name = "is_api_endpoint")
    @Builder.Default
    private Boolean isApiEndpoint = false;

    @Column(name = "http_method", length = 10)
    private String httpMethod; // GET, POST, PUT, DELETE

    @Column(length = 255)
    private String path;
}
