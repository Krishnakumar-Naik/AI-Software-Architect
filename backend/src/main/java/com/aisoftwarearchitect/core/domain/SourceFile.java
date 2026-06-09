package com.aisoftwarearchitect.core.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "source_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_name", nullable = false, length = 100)
    private String fileName;

    @Column(name = "file_type", nullable = false, length = 20)
    private String fileType; // JAVA, JAVASCRIPT, TYPESCRIPT, PYTHON, OTHER

    @Column(columnDefinition = "LONGTEXT")
    private String content;
}
