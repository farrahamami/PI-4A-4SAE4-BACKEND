package com.esprit.microservice.pidev.dto;
import com.esprit.microservice.pidev.Entities.Role;

public class AuthResponse {
    private String token;
    private Role role;
    private Integer userId;

    public AuthResponse(String token, Role role, Integer userId) {
        this.token = token;
        this.role = role;
        this.userId = userId;
    }

    // getters and setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
}