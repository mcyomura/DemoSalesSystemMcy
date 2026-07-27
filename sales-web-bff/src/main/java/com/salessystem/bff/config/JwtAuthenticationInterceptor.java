package com.salessystem.bff.config;

import com.nimbusds.jwt.JWTClaimsSet;
import com.salessystem.bff.service.JwtValidationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor extracting and validating JWT Bearer tokens for protected endpoints.
 */
@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private final JwtValidationService jwtValidationService;

    // Added @Lazy annotation to break circular dependency during Spring context startup
    public JwtAuthenticationInterceptor(@Lazy JwtValidationService jwtValidationService) {
        this.jwtValidationService = jwtValidationService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Missing or invalid Authorization header");
            return false;
        }

        String token = authHeader.substring(7);

        try {
            JWTClaimsSet claims = jwtValidationService.validateToken(token);
            // Attaches authenticated user id to the request context
            request.setAttribute("authenticatedUserId", claims.getSubject());

            return true;
        } catch (Exception e) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Authentication failed: " + e.getMessage());
            return false;
        }
    }
}