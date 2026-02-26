package com.capgemini.mprs.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF so POST from Swagger (no CSRF token) works
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                // Allow Swagger & your bulk upload without auth
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/api/v1/transactions/bulk",
                                "/api/v1/transactions/**",
                                "/api/v1/payouts/**",
                                "/api/v1/reconciliations/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                );

        // If you also had login pages, you could add http.formLogin(); optionally
        return http.build();
    }
}

