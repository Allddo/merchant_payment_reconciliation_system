package com.capgemini.mprs.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-minutes}") long expirationMinutes
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(String subject, List<String> roles) {
        long now = System.currentTimeMillis();
        long expiresAt = now + (expirationMinutes * 60 * 1000);

        return Jwts.builder()
                .setSubject(subject)               // who the token is for
                .claim("roles", roles)             // user roles
                .setIssuedAt(new Date(now))        // when issued
                .setExpiration(new Date(expiresAt))// when it expires
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}