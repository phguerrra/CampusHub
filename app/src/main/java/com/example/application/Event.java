package com.example.application;

import com.google.firebase.Timestamp;

public class Event {
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_CLOSED = "CLOSED";

    private String id;
    private String name;
    private String description;
    private Timestamp date;
    private String time;
    private String location;
    private long availableSlots;
    private long maxCapacity;
    private String category;
    private Double latitude;
    private Double longitude;
    private String createdBy;
    private String status;

    public Event() {
        // Construtor vazio obrigatório para a conversão de documentos do Firestore.
    }

    public Event(String id, String name, String description, Timestamp date, String time,
                 String location, long availableSlots, String category, Double latitude, Double longitude) {
        this(id, name, description, date, time, location, availableSlots, availableSlots, category, latitude, longitude, null, STATUS_PUBLISHED);
    }

    public Event(String id, String name, String description, Timestamp date, String time,
                 String location, long availableSlots, long maxCapacity, String category,
                 Double latitude, Double longitude, String createdBy, String status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.date = date;
        this.time = time;
        this.location = location;
        this.availableSlots = availableSlots;
        this.maxCapacity = maxCapacity;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdBy = createdBy;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public long getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(long availableSlots) {
        this.availableSlots = availableSlots;
    }

    public long getMaxCapacity() {
        return maxCapacity > 0 ? maxCapacity : availableSlots;
    }

    public void setMaxCapacity(long maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getLatitude() {
        return latitude != null ? latitude : 0.0;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude != null ? longitude : 0.0;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getStatus() {
        return status != null ? status : STATUS_PUBLISHED;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isClosed() {
        return STATUS_CLOSED.equalsIgnoreCase(getStatus());
    }
}
