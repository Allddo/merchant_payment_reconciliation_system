package com.capgemini.mprs.Tests;

import com.capgemini.mprs.controllers.AuthController;
import com.capgemini.mprs.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void tokenViaQuery_returnsToken() throws Exception {
        // Arrange
        String user = "alice";
        String roles = "FINANCE_ANALYST,ADMIN";
        List<String> parsedRoles = List.of("FINANCE_ANALYST", "ADMIN");

        when(jwtService.generateToken(user, parsedRoles))
                .thenReturn("mock.jwt.token");

        // Act + Assert
        mockMvc.perform(post("/api/v1/auth/token")
                        .param("user", user)
                        .param("roles", roles)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock.jwt.token"));

        verify(jwtService).generateToken(user, parsedRoles);
    }

    @Test
    void tokenViaQuery_usesDefaultRoleWhenMissing() throws Exception {
        // Arrange
        String user = "bob";
        List<String> defaultRoles = List.of("FINANCE_ANALYST");

        when(jwtService.generateToken(user, defaultRoles))
                .thenReturn("default.jwt.token");

        // Act + Assert
        // No 'roles' param → should use FINANCE_ANALYST
        mockMvc.perform(post("/api/v1/auth/token")
                        .param("user", user)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("default.jwt.token"));

        verify(jwtService).generateToken(user, defaultRoles);
    }
}