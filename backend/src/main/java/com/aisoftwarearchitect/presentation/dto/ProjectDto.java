package com.aisoftwarearchitect.presentation.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDto {
    private Long id;
    private String name;
    private String description;
    private String repositoryUrl;
    private String repoOwner;
    private String repositoryName;
    private String branchName;
    private Boolean isPublic;
    private LocalDateTime createdAt;
}
