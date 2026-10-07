package com.codesim.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role = "USER"; // USER, ADMIN, RECRUITER

    @Column(name = "elo_rating", nullable = false)
    private Double eloRating = 1000.0;

    @Column(name = "total_interviews", nullable = false)
    private Integer totalInterviews = 0;

    @Column(name = "total_questions_solved", nullable = false)
    private Integer totalQuestionsSolved = 0;

    @Column(name = "avg_score", nullable = false)
    private Double avgScore = 0.0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_active", nullable = false)
    private Instant lastActive = Instant.now();

    @Column(name = "is_active_account", nullable = false)
    private Boolean isActiveAccount = true;

    @Column(columnDefinition = "TEXT")
    private String bio = "";

    private String college = "";

    @Column(name = "preferred_language")
    private String preferredLanguage = "python";

    @Column(name = "theme_preference", nullable = false)
    private String themePreference = "dark";

    @Column(name = "has_completed_assessment", nullable = false)
    private Boolean hasCompletedAssessment = false;

    public Boolean getHasCompletedAssessment() { return hasCompletedAssessment; }
    public void setHasCompletedAssessment(Boolean hasCompletedAssessment) { this.hasCompletedAssessment = hasCompletedAssessment; }

    public String getSkillLevel() {
        if (eloRating < 1100) return "Beginner";
        if (eloRating < 1400) return "Intermediate";
        if (eloRating < 1700) return "Advanced";
        return "Expert";
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Double getEloRating() { return eloRating; }
    public void setEloRating(Double eloRating) { this.eloRating = eloRating; }
    public Integer getTotalInterviews() { return totalInterviews; }
    public void setTotalInterviews(Integer totalInterviews) { this.totalInterviews = totalInterviews; }
    public Integer getTotalQuestionsSolved() { return totalQuestionsSolved; }
    public void setTotalQuestionsSolved(Integer totalQuestionsSolved) { this.totalQuestionsSolved = totalQuestionsSolved; }
    public Double getAvgScore() { return avgScore; }
    public void setAvgScore(Double avgScore) { this.avgScore = avgScore; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getLastActive() { return lastActive; }
    public void setLastActive(Instant lastActive) { this.lastActive = lastActive; }
    public Boolean getIsActiveAccount() { return isActiveAccount; }
    public void setIsActiveAccount(Boolean isActiveAccount) { this.isActiveAccount = isActiveAccount; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }
    public String getThemePreference() { return themePreference; }
    public void setThemePreference(String themePreference) { this.themePreference = themePreference; }
}
