package com.esprit.microservice.pidev.Services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    // Must be at least 32 chars for HS256
    private final String SECRET = "super-secret-key-that-is-at-least-32-chars!";

    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    /**
     * Generates a JWT containing email (subject), role, AND userId.
     * The frontend decodes the payload client-side to retrieve userId
     * without an extra HTTP round-trip.
     */
    public String generateToken(String email, String role, Integer userId) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .claim("userId", userId)          // ← NEW: needed by Angular AuthService
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 1 day
                .signWith(key)
                .compact();
    }
}