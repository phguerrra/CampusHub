package com.example.application;

import com.google.firebase.Timestamp;

public class EventAudio {
    private String id;
    private String eventId;
    private String userId;
    private String authorName;
    private String audioPath;
    private Timestamp createdAt;

    public EventAudio() {
        // Construtor vazio obrigatório para o Firestore
    }

    public EventAudio(String id, String eventId, String userId, String authorName, String audioPath, Timestamp createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.userId = userId;
        this.authorName = authorName;
        this.audioPath = audioPath;
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

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
