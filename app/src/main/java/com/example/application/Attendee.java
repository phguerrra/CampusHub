package com.example.application;

import com.google.firebase.Timestamp;

public class Attendee {
    private String subscriptionId;
    private String userId;
    private String name;
    private String email;
    private Timestamp subscribedAt;
    private boolean checkedIn;
    private Timestamp checkedInAt;

    public Attendee() {
        // Construtor vazio obrigatório para conversão de documentos
    }

    public Attendee(String subscriptionId, String userId, String name, String email,
                    Timestamp subscribedAt, boolean checkedIn, Timestamp checkedInAt) {
        this.subscriptionId = subscriptionId;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.subscribedAt = subscribedAt;
        this.checkedIn = checkedIn;
        this.checkedInAt = checkedInAt;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
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

    public Timestamp getSubscribedAt() {
        return subscribedAt;
    }

    public void setSubscribedAt(Timestamp subscribedAt) {
        this.subscribedAt = subscribedAt;
    }

    public boolean isCheckedIn() {
        return checkedIn;
    }

    public void setCheckedIn(boolean checkedIn) {
        this.checkedIn = checkedIn;
    }

    public Timestamp getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(Timestamp checkedInAt) {
        this.checkedInAt = checkedInAt;
    }
}
