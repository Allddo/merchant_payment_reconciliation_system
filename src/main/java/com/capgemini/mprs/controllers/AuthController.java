package com.capgemini.mprs.controllers;

import com.capgemini.mprs.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/token")
    public ResponseEntity<Map<String, String>> tokenViaQuery(
            @RequestParam String user,
            @RequestParam(defaultValue = "FINANCE_ANALYST") String roles
    ) {
        List<String> roleList = List.of(roles.split("\\s*,\\s*"));
        String jwt = jwtService.generateToken(user, roleList);
        return ResponseEntity.ok(Map.of("token", jwt));
    }
}