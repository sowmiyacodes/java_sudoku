package com.sudoku.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ml_prediction_logs")
public class MLPredictionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "predicted_skill", length = 32, nullable = false)
    private String predictedSkill;

    @Column(name = "confidence", nullable = false)
    private double confidence;

    @Column(name = "model_version", length = 32, nullable = false)
    private String modelVersion;

    @Lob
    @Column(name = "feature_snapshot_json", columnDefinition = "TEXT")
    private String featureSnapshotJson;

    @Column(name = "prediction_time", nullable = false, updatable = false)
    private LocalDateTime predictionTime;

    public MLPredictionLog() {}

    public MLPredictionLog(Long userId, String predictedSkill, double confidence,
                           String modelVersion, String featureSnapshotJson) {
        this.userId = userId;
        this.predictedSkill = predictedSkill;
        this.confidence = confidence;
        this.modelVersion = modelVersion;
        this.featureSnapshotJson = featureSnapshotJson;
        this.predictionTime = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.predictionTime == null) {
            this.predictionTime = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getPredictedSkill() { return predictedSkill; }
    public void setPredictedSkill(String predictedSkill) { this.predictedSkill = predictedSkill; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public String getFeatureSnapshotJson() { return featureSnapshotJson; }
    public void setFeatureSnapshotJson(String featureSnapshotJson) { this.featureSnapshotJson = featureSnapshotJson; }

    public LocalDateTime getPredictionTime() { return predictionTime; }
}
