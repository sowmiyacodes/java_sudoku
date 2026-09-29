package com.sudoku.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recommendations")
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "recommendation_type", length = 64, nullable = false)
    private String recommendationType = "DIFFICULTY_PROGRESSION";

    @Column(name = "recommended_difficulty", length = 32, nullable = false)
    private String recommendedDifficulty;

    @Column(name = "reason", length = 1000, nullable = false)
    private String reason;

    @Column(name = "confidence", nullable = false)
    private double confidence;

    @Column(name = "skill_level", length = 32)
    private String skillLevel;

    @Column(name = "model_version", length = 32)
    private String modelVersion = "v1";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Recommendation() {}

    public Recommendation(Long userId, String recommendedDifficulty, String reason,
                          double confidence, String skillLevel, String modelVersion) {
        this.userId = userId;
        this.recommendedDifficulty = recommendedDifficulty;
        this.reason = reason;
        this.confidence = confidence;
        this.skillLevel = skillLevel;
        this.modelVersion = modelVersion;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRecommendationType() { return recommendationType; }
    public void setRecommendationType(String recommendationType) { this.recommendationType = recommendationType; }

    public String getRecommendedDifficulty() { return recommendedDifficulty; }
    public void setRecommendedDifficulty(String recommendedDifficulty) { this.recommendedDifficulty = recommendedDifficulty; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getSkillLevel() { return skillLevel; }
    public void setSkillLevel(String skillLevel) { this.skillLevel = skillLevel; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
