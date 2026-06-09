package com.aisoftwarearchitect.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitHubProjectRequest {
    @NotBlank(message = "Project name is required")
    private String name;

    private String description;

    @NotBlank(message = "GitHub repository URL is required")
    private String cloneUrl;
}
