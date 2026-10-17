package com.example.application;

import com.google.firebase.Timestamp;

public class EventPhoto {
    private String id;
    private String eventId;
    private String userId;
    private String authorName;
    private String photoPath;
    private Timestamp createdAt;

    public EventPhoto() {
        // Construtor vazio obrigatório para o Firestore
    }

    public EventPhoto(String id, String eventId, String userId, String authorName, String photoPath, Timestamp createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.userId = userId;
        this.authorName = authorName;
        this.photoPath = photoPath;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
