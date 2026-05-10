// Initiative.java
package com.example.ecoinitiatives.model;

import java.util.HashMap;
import java.util.Map;

public class Initiative {
    private String id;
    private String title;
    private String description;
    private String userId;
    private String status; // "moderation", "approved", "rejected"
    private long createdAt;
    private int likesCount;
    private int responsesCount;
    private Map<String, Boolean> likes;
    private Map<String, String> responses;

    public Initiative() {
        this.likes = new HashMap<>();
        this.responses = new HashMap<>();
        this.likesCount = 0;
        this.responsesCount = 0;
        this.status = "moderation";
        this.createdAt = System.currentTimeMillis();
    }

    public Initiative(String id, String title, String description, String userId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.userId = userId;
        this.status = "moderation";
        this.createdAt = System.currentTimeMillis();
        this.likes = new HashMap<>();
        this.responses = new HashMap<>();
        this.likesCount = 0;
        this.responsesCount = 0;
    }

    // Getters и Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }
    public int getResponsesCount() { return responsesCount; }
    public void setResponsesCount(int responsesCount) { this.responsesCount = responsesCount; }
    public Map<String, Boolean> getLikes() { return likes; }
    public void setLikes(Map<String, Boolean> likes) { this.likes = likes; }
    public Map<String, String> getResponses() { return responses; }
    public void setResponses(Map<String, String> responses) { this.responses = responses; }
}