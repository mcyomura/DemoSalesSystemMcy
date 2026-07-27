package com.salessystem.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration applying security interceptor to protected cart routes.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthenticationInterceptor jwtInterceptor;
    private final String authPath;

    public WebMvcConfig(JwtAuthenticationInterceptor jwtInterceptor, @Value("${app.api.path.bff}") String authPath) {
        this.jwtInterceptor = jwtInterceptor;
        this.authPath = authPath;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Protects only cart endpoints requiring authenticated user
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns(authPath + "/cart/checkout", authPath + "/cart/{id:[0-9]+}");
    }
}