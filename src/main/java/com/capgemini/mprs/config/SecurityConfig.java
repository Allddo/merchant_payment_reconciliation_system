package com.capgemini.mprs.config;

import com.capgemini.mprs.security.JwtAuthenticationFilter;
import org.springframework.http.HttpMethod;
import com.capgemini.mprs.security.RestAuthEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity // optional, lets you use @PreAuthorize on controllers
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RestAuthEntryPoint authEntryPoint,
            @Value("${security.jwt.secret}") String jwtSecret
    ) throws Exception {

        var jwtFilter = new JwtAuthenticationFilter(jwtSecret);

        http
                .cors(Customizer.withDefaults())
                // Swagger needs CSRF disabled for easy POST testing
                .csrf(csrf -> csrf.disable())

                // Stateless API: no HTTP sessions
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Consistent 401/403 JSON responses
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(403);
                            res.setContentType("application/json");
                            res.getWriter().write("""
                      {"timestamp":"%s","status":403,"error":"Forbidden","message":"Access denied","path":"%s"}
                      """.formatted(java.time.Instant.now(), req.getRequestURI()));
                        })
                )

                // RBAC rules from PDF
                .authorizeHttpRequests(auth -> auth

                        // ✅ Allow CORS preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public: Swagger UI + Health + token minting
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/actuator/health",
                                "/api/v1/auth/token"
                        ).permitAll()

                        // ADMIN or SYSTEM can upload/trigger
                        .requestMatchers("/api/v1/transactions/**").hasAnyRole("ADMIN","SYSTEM")
                        .requestMatchers("/api/v1/payouts/**").hasAnyRole("ADMIN","SYSTEM")
                        .requestMatchers("/api/v1/reconciliation/run").hasAnyRole("ADMIN","SYSTEM")

                        // Read-only for analysts (+ admin/system)
                        .requestMatchers(
                                "/api/v1/reconciliation/**",
                                "/api/v1/transactions", // GET list
                                "/api/v1/payouts"       // GET list
                        ).hasAnyRole("FINANCE_ANALYST","ADMIN","SYSTEM")

                        // Everything else must be authenticated
                        .anyRequest().authenticated()
                )

                // Install our JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}