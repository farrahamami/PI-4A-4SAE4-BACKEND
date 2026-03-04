package com.esprit.microservice.pidev.dto;

public class AuthResponse {
    private Integer id;
    private String token;
    private String role;

    public AuthResponse(Integer id, String token, String role) {
        this.id = id;
        this.token = token;
        this.role = role;
    }

    // Getters and setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}