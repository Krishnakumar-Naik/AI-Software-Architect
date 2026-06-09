package com.aisoftwarearchitect.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssistantChatRequest {
    @NotNull(message = "Project ID is required")
    private Long projectId;
    
    private Long conversationId;
    
    private String message;
}
