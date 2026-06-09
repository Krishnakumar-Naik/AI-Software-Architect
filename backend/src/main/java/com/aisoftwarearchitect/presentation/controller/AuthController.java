package com.aisoftwarearchitect.presentation.controller;

import com.aisoftwarearchitect.core.domain.User;
import com.aisoftwarearchitect.core.service.AuthService;
import com.aisoftwarearchitect.core.service.UserService;
import com.aisoftwarearchitect.infrastructure.security.JwtCookieUtils;
import com.aisoftwarearchitect.infrastructure.security.JwtTokenProvider;
import com.aisoftwarearchitect.presentation.dto.LoginRequest;
import com.aisoftwarearchitect.presentation.dto.MessageResponse;
import com.aisoftwarearchitect.presentation.dto.RegisterRequest;
import com.aisoftwarearchitect.presentation.dto.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtCookieUtils jwtCookieUtils;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Request to register user: {}", registerRequest.getUsername());
        User user = authService.registerUser(registerRequest);
        
        // Auto-login after registration by generating token
        String jwt = jwtTokenProvider.generateTokenFromUsername(user.getUsername());
        ResponseCookie jwtCookie = jwtCookieUtils.generateJwtCookie(jwt);

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()))
                .build();

        log.info("User registered successfully: {}", user.getUsername());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(userDto);
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Request to authenticate user: {}", loginRequest.getUsername());
        Authentication authentication = authService.authenticateUser(loginRequest);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken(authentication);
        ResponseCookie jwtCookie = jwtCookieUtils.generateJwtCookie(jwt);

        User user = userService.getUserByUsername(jwtTokenProvider.getUsernameFromToken(jwt));
        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()))
                .build();

        log.info("User authenticated successfully: {}", loginRequest.getUsername());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(userDto);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        log.info("Request to logout user");
        ResponseCookie cookie = jwtCookieUtils.getCleanJwtCookie();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new MessageResponse("You have been signed out."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(new MessageResponse("Not authenticated"));
        }
        
        User user = userService.getUserByUsername(principal.getName());
        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()))
                .build();
                
        return ResponseEntity.ok(userDto);
    }
}
