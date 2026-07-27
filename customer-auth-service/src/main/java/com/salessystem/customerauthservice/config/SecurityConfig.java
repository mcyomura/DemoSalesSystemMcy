package com.salessystem.customerauthservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration allowing access to authentication endpoints coming from other ms.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final String authBasePath;

    public SecurityConfig(@Value("${app.api.path.auth}") String authBasePath) {
        this.authBasePath = authBasePath;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        String formattedPath = authBasePath.startsWith("/") ? authBasePath : "/" + authBasePath;
        String authPattern = formattedPath.endsWith("/") ? formattedPath + "**" : formattedPath + "/**";

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(authPattern, "/error").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}