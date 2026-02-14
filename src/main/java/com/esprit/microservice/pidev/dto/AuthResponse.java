package com.esprit.microservice.pidev.dto;
import com.esprit.microservice.pidev.Entities.Role;

public class AuthResponse {
    private String token;
    private Role role; // include role

    public AuthResponse(String token, Role role) {
        this.token = token;
        this.role = role;
    }

    // getters and setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}