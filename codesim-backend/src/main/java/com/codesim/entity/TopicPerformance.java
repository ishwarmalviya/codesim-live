package com.codesim.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "topic_performances")
public class TopicPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String topic;

    @Column(name = "questions_attempted", nullable = false)
    private Integer questionsAttempted = 0;

    @Column(name = "questions_solved", nullable = false)
    private Integer questionsSolved = 0;

    @Column(name = "avg_score", nullable = false)
    private Double avgScore = 0.0;

    @Column(nullable = false)
    private Double proficiency = 0.0; // 0-100

    @Column(name = "last_practiced", nullable = false)
    private Instant lastPracticed = Instant.now();

    public Double getSuccessRate() {
        if (questionsAttempted == 0) return 0.0;
        return Math.round((questionsSolved / (double) questionsAttempted) * 1000) / 10.0;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public Integer getQuestionsAttempted() { return questionsAttempted; }
    public void setQuestionsAttempted(Integer questionsAttempted) { this.questionsAttempted = questionsAttempted; }
    public Integer getQuestionsSolved() { return questionsSolved; }
    public void setQuestionsSolved(Integer questionsSolved) { this.questionsSolved = questionsSolved; }
    public Double getAvgScore() { return avgScore; }
    public void setAvgScore(Double avgScore) { this.avgScore = avgScore; }
    public Double getProficiency() { return proficiency; }
    public void setProficiency(Double proficiency) { this.proficiency = proficiency; }
    public Instant getLastPracticed() { return lastPracticed; }
    public void setLastPracticed(Instant lastPracticed) { this.lastPracticed = lastPracticed; }
}
