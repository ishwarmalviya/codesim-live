package com.codesim.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "interviews")
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(nullable = false)
    private String status = "active"; // active, completed, abandoned

    @Column(name = "interview_type", nullable = false)
    private String interviewType = "adaptive"; // adaptive, topic, mock

    @Column(name = "selected_topic")
    private String selectedTopic = "";

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions = 5;

    @Column(name = "questions_answered", nullable = false)
    private Integer questionsAnswered = 0;

    @Column(nullable = false)
    private Double score = 0.0;

    @Column(name = "max_score", nullable = false)
    private Double maxScore = 0.0;

    @Column(name = "elo_before", nullable = false)
    private Double eloBefore = 1000.0;

    @Column(name = "elo_after", nullable = false)
    private Double eloAfter = 1000.0;

    @Column(name = "overall_feedback", columnDefinition = "TEXT")
    private String overallFeedback = "";

    @Column(columnDefinition = "TEXT")
    private String strengths = "";

    @Column(columnDefinition = "TEXT")
    private String improvements = "";

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary = "";

    @Column(name = "selected_topics", columnDefinition = "TEXT")
    private String selectedTopics = "[]";

    @Column(name = "target_company_id")
    private Long targetCompanyId;

    @Column(name = "target_role")
    private String targetRole = "";

    @OneToMany(mappedBy = "interview", fetch = FetchType.LAZY)
    @OrderBy("questionOrder ASC")
    private List<InterviewQuestion> interviewQuestions = new ArrayList<>();

    public Double getScorePercent() {
        if (maxScore == 0) return 0.0;
        return Math.round((score / maxScore) * 1000) / 10.0;
    }

    public Double getDurationMinutes() {
        if (completedAt == null) return 0.0;
        long seconds = ChronoUnit.SECONDS.between(startedAt, completedAt);
        return Math.round((seconds / 60.0) * 10) / 10.0;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getInterviewType() { return interviewType; }
    public void setInterviewType(String interviewType) { this.interviewType = interviewType; }
    public String getSelectedTopic() { return selectedTopic; }
    public void setSelectedTopic(String selectedTopic) { this.selectedTopic = selectedTopic; }
    public Integer getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; }
    public Integer getQuestionsAnswered() { return questionsAnswered; }
    public void setQuestionsAnswered(Integer questionsAnswered) { this.questionsAnswered = questionsAnswered; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public Double getMaxScore() { return maxScore; }
    public void setMaxScore(Double maxScore) { this.maxScore = maxScore; }
    public Double getEloBefore() { return eloBefore; }
    public void setEloBefore(Double eloBefore) { this.eloBefore = eloBefore; }
    public Double getEloAfter() { return eloAfter; }
    public void setEloAfter(Double eloAfter) { this.eloAfter = eloAfter; }
    public String getOverallFeedback() { return overallFeedback; }
    public void setOverallFeedback(String overallFeedback) { this.overallFeedback = overallFeedback; }
    public String getStrengths() { return strengths; }
    public void setStrengths(String strengths) { this.strengths = strengths; }
    public String getImprovements() { return improvements; }
    public void setImprovements(String improvements) { this.improvements = improvements; }
    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
    public String getSelectedTopics() { return selectedTopics; }
    public void setSelectedTopics(String selectedTopics) { this.selectedTopics = selectedTopics; }
    public Long getTargetCompanyId() { return targetCompanyId; }
    public void setTargetCompanyId(Long targetCompanyId) { this.targetCompanyId = targetCompanyId; }
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
    public List<InterviewQuestion> getInterviewQuestions() { return interviewQuestions; }
    public void setInterviewQuestions(List<InterviewQuestion> interviewQuestions) { this.interviewQuestions = interviewQuestions; }
}
