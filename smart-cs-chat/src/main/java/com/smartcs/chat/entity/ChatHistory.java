package com.smartcs.chat.entity;

import java.time.LocalDateTime;

public class ChatHistory {
    private Long id;
    private Long userId;
    private String sessionId;
    private String question;
    private String answer;
    private String model;
    private Integer tokensUsed;
    private Integer latencyMs;
    private Integer rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ChatHistory() {}

    public ChatHistory(Long userId, String sessionId, String question, String answer, String model, Integer tokensUsed, Integer latencyMs) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.question = question;
        this.answer = answer;
        this.model = model;
        this.tokensUsed = tokensUsed;
        this.latencyMs = latencyMs;
        this.rating = 0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(Integer tokensUsed) { this.tokensUsed = tokensUsed; }

    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
