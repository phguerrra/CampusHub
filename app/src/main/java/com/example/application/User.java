package com.example.application;

import com.google.firebase.Timestamp;

public class User {
    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_ORGANIZER = "ORGANIZER";

    private String userId;
    private String name;
    private String email;
    private String role;
    private Timestamp createdAt;

    public User() {
        // Construtor vazio obrigatório para o Firestore
    }

    public User(String userId, String name, String email, String role, Timestamp createdAt) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role != null ? role : ROLE_STUDENT;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isOrganizer() {
        return ROLE_ORGANIZER.equalsIgnoreCase(getRole());
    }
}
