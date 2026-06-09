package com.aisoftwarearchitect.infrastructure.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

@Component
public class JwtCookieUtils {

    @Value("${spring.security.jwt.cookie-name:jwt-token}")
    private String jwtCookieName;

    @Value("${spring.security.jwt.expiration:86400000}")
    private Long jwtExpirationMs;

    @Value("${spring.security.jwt.cookie-secure:false}")
    private boolean jwtCookieSecure;

    public String getJwtFromCookies(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, jwtCookieName);
        if (cookie != null) {
            return cookie.getValue();
        }
        return null;
    }

    public ResponseCookie generateJwtCookie(String token) {
        return ResponseCookie.from(jwtCookieName, token)
                .path("/")
                .maxAge(jwtExpirationMs / 1000)
                .httpOnly(true)
                .secure(jwtCookieSecure)
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie getCleanJwtCookie() {
        return ResponseCookie.from(jwtCookieName, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(jwtCookieSecure)
                .sameSite("Lax")
                .build();
    }
}
