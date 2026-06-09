package com.aisoftwarearchitect.presentation.controller;

import com.aisoftwarearchitect.core.domain.User;
import com.aisoftwarearchitect.core.service.UserService;
import com.aisoftwarearchitect.infrastructure.security.JwtCookieUtils;
import com.aisoftwarearchitect.infrastructure.security.JwtTokenProvider;
import com.aisoftwarearchitect.presentation.dto.ChangePasswordRequest;
import com.aisoftwarearchitect.presentation.dto.MessageResponse;
import com.aisoftwarearchitect.presentation.dto.UpdateProfileRequest;
import com.aisoftwarearchitect.presentation.dto.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtCookieUtils jwtCookieUtils;

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(Principal principal, 
                                           @Valid @RequestBody UpdateProfileRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).body(new MessageResponse("Not authenticated"));
        }
        
        String currentUsername = principal.getName();
        log.info("Request by user {} to update profile", currentUsername);
        
        User updatedUser = userService.updateProfile(currentUsername, request);
        
        UserDto userDto = UserDto.builder()
                .id(updatedUser.getId())
                .username(updatedUser.getUsername())
                .email(updatedUser.getEmail())
                .roles(updatedUser.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()))
                .build();
                
        // If the username changed, reissue the JWT cookie
        if (!updatedUser.getUsername().equalsIgnoreCase(currentUsername)) {
            String jwt = jwtTokenProvider.generateTokenFromUsername(updatedUser.getUsername());
            ResponseCookie jwtCookie = jwtCookieUtils.generateJwtCookie(jwt);
            log.info("Reissued JWT cookie for updated username: {}", updatedUser.getUsername());
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .body(userDto);
        }
        
        return ResponseEntity.ok(userDto);
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(Principal principal, 
                                            @Valid @RequestBody ChangePasswordRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).body(new MessageResponse("Not authenticated"));
        }
        
        String currentUsername = principal.getName();
        log.info("Request by user {} to change password", currentUsername);
        
        userService.changePassword(currentUsername, request);
        log.info("Password changed successfully for user {}", currentUsername);
        
        return ResponseEntity.ok(new MessageResponse("Password changed successfully"));
    }
}
