package com.sudoku.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "player_preferences", uniqueConstraints = {
        @UniqueConstraint(name = "uk_player_pref_user", columnNames = {"user_id"})
})
public class PlayerPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "preferred_difficulty", length = 32)
    private String preferredDifficulty = "Medium";

    @Column(name = "preferred_game_mode", length = 32)
    private String preferredGameMode = "SOLO";

    @Column(name = "target_difficulty", length = 32)
    private String targetDifficulty = "Hard";

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public PlayerPreference() {}

    public PlayerPreference(Long userId) {
        this.userId = userId;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getPreferredDifficulty() { return preferredDifficulty; }
    public void setPreferredDifficulty(String preferredDifficulty) { this.preferredDifficulty = preferredDifficulty; }

    public String getPreferredGameMode() { return preferredGameMode; }
    public void setPreferredGameMode(String preferredGameMode) { this.preferredGameMode = preferredGameMode; }

    public String getTargetDifficulty() { return targetDifficulty; }
    public void setTargetDifficulty(String targetDifficulty) { this.targetDifficulty = targetDifficulty; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
