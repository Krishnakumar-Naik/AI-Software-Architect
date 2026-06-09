package com.aisoftwarearchitect.core.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "class_metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_file_id", nullable = false)
    private SourceFile sourceFile;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String type; // CLASS, INTERFACE

    @Column(nullable = false, length = 20)
    private String stereotype; // CONTROLLER, SERVICE, REPOSITORY, OTHER

    @Column(name = "package_name")
    private String packageName;

    @OneToMany(mappedBy = "classMetadata", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MethodMetadata> methods = new ArrayList<>();
}
