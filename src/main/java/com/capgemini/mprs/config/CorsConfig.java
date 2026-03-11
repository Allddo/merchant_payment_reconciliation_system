package com.capgemini.mprs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration cfg = new CorsConfiguration();

        // Allow your React dev server
        cfg.setAllowedOrigins(java.util.List.of("http://localhost:3000", "http://127.0.0.1:3000"));
        // Methods your frontend will use
        cfg.setAllowedMethods(java.util.List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        // Headers your frontend will send (Authorization is required for JWT)
        cfg.setAllowedHeaders(java.util.List.of("Authorization","Content-Type","Accept"));
        // Optional: expose any headers you want the browser to read
        cfg.setExposedHeaders(java.util.List.of("Location"));
        // Using Bearer tokens (no cookies), so keep credentials off
        cfg.setAllowCredentials(false);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);

        return source;
    }

}
