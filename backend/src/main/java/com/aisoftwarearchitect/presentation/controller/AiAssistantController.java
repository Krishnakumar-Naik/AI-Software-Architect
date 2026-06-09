package com.aisoftwarearchitect.presentation.controller;

import com.aisoftwarearchitect.core.domain.*;
import com.aisoftwarearchitect.core.repository.*;
import com.aisoftwarearchitect.core.service.AiAssistantService;
import com.aisoftwarearchitect.core.service.UserService;
import com.aisoftwarearchitect.presentation.dto.AssistantChatRequest;
import com.aisoftwarearchitect.presentation.dto.MessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
@Slf4j
public class AiAssistantController {

    private final UserService userService;
    private final AiAssistantService aiAssistantService;
    private final ProjectRepository projectRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @PostMapping("/chat")
    public ResponseEntity<?> chat(Principal principal, @Valid @RequestBody AssistantChatRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userService.getUserByUsername(principal.getName());
        String msg = (request.getMessage() == null || request.getMessage().trim().isEmpty()) 
                ? "Explain the architecture of this project." 
                : request.getMessage();

        Map<String, Object> response = aiAssistantService.processAssistantChat(
                request.getProjectId(), request.getConversationId(), user, msg, "CHAT");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/interview")
    public ResponseEntity<?> generateInterview(Principal principal, @Valid @RequestBody AssistantChatRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userService.getUserByUsername(principal.getName());
        String msg = (request.getMessage() == null || request.getMessage().trim().isEmpty()) 
                ? "Generate technical project-based interview screening questions." 
                : request.getMessage();

        Map<String, Object> response = aiAssistantService.processAssistantChat(
                request.getProjectId(), request.getConversationId(), user, msg, "INTERVIEW");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/review")
    public ResponseEntity<?> generateReview(Principal principal, @Valid @RequestBody AssistantChatRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userService.getUserByUsername(principal.getName());
        String msg = (request.getMessage() == null || request.getMessage().trim().isEmpty()) 
                ? "Generate a complete design architecture review focusing on tight coupling and design metrics." 
                : request.getMessage();

        Map<String, Object> response = aiAssistantService.processAssistantChat(
                request.getProjectId(), request.getConversationId(), user, msg, "REVIEW");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/security")
    public ResponseEntity<?> generateSecurityReview(Principal principal, @Valid @RequestBody AssistantChatRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userService.getUserByUsername(principal.getName());
        String msg = (request.getMessage() == null || request.getMessage().trim().isEmpty()) 
                ? "Generate a comprehensive security review for the API endpoints and layers." 
                : request.getMessage();

        Map<String, Object> response = aiAssistantService.processAssistantChat(
                request.getProjectId(), request.getConversationId(), user, msg, "SECURITY");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resume-bullets")
    public ResponseEntity<?> generateResumeBullets(Principal principal, @RequestParam("projectId") Long projectId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        log.info("Generating resume bullets for project ID: {}", projectId);
        String bullets = aiAssistantService.generateResumeBullets(projectId);
        return ResponseEntity.ok(Map.of("bullets", bullets));
    }

    @PostMapping("/project-summary")
    public ResponseEntity<?> generateProjectSummary(Principal principal, @RequestParam("projectId") Long projectId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        log.info("Generating project summary report for project ID: {}", projectId);
        String summary = aiAssistantService.generateProjectSummary(projectId);
        return ResponseEntity.ok(Map.of("summary", summary));
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(Principal principal, @RequestParam("projectId") Long projectId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userService.getUserByUsername(principal.getName());
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }

        List<Conversation> convs = conversationRepository.findByProjectAndUserOrderByUpdatedAtDesc(project, user);
        List<Map<String, Object>> res = convs.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("title", c.getTitle());
            m.put("updatedAt", c.getUpdatedAt());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(res);
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<?> getConversation(Principal principal, @PathVariable Long id) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userService.getUserByUsername(principal.getName());
        Conversation conversation = conversationRepository.findById(id).orElse(null);
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }

        // Verify conversation ownership
        if (!conversation.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(new MessageResponse("Access Denied"));
        }

        List<Message> history = messageRepository.findByConversationOrderByTimestampAsc(conversation);
        List<Map<String, Object>> messagesList = history.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("sender", m.getSender());
            map.put("content", m.getContent());
            map.put("timestamp", m.getTimestamp());
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> res = new HashMap<>();
        res.put("id", conversation.getId());
        res.put("title", conversation.getTitle());
        res.put("projectId", conversation.getProject().getId());
        res.put("messages", messagesList);

        return ResponseEntity.ok(res);
    }
}
