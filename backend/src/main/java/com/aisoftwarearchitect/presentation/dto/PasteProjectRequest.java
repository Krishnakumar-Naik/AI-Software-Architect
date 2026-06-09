package com.aisoftwarearchitect.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasteProjectRequest {
    @NotBlank(message = "Project name is required")
    private String name;

    private String description;

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "File type is required (e.g. JAVA, PYTHON, TYPESCRIPT)")
    private String fileType;

    @NotBlank(message = "Code content is required")
    private String codeContent;
}
