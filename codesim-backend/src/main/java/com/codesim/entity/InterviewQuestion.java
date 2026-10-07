package com.codesim.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "interview_questions")
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder = 1;

    @Column(name = "user_code", columnDefinition = "TEXT")
    private String userCode = "";

    @Column(nullable = false)
    private Double score = 0.0;

    @Column(name = "max_score", nullable = false)
    private Double maxScore = 10.0;

    @Column(name = "tests_passed", nullable = false)
    private Integer testsPassed = 0;

    @Column(name = "tests_total", nullable = false)
    private Integer testsTotal = 0;

    @Column(name = "time_taken_seconds", nullable = false)
    private Integer timeTakenSeconds = 0;

    @Column(name = "hints_used", nullable = false)
    private Integer hintsUsed = 0;

    @Column(nullable = false)
    private String status = "pending"; // pending, solved, failed, skipped

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback = "";

    @Column(name = "code_quality_score", nullable = false)
    private Double codeQualityScore = 0.0;

    @Column(name = "complexity_analysis", columnDefinition = "TEXT")
    private String complexityAnalysis = "";

    @Column(name = "execution_output", columnDefinition = "TEXT")
    private String executionOutput = "";

    @Column(name = "user_elo_delta", nullable = false)
    private Double userEloDelta = 0.0;

    @Column(name = "question_elo_delta", nullable = false)
    private Double questionEloDelta = 0.0;

    public Double getScorePercent() {
        if (maxScore == 0) return 0.0;
        return Math.round((score / maxScore) * 1000) / 10.0;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Interview getInterview() { return interview; }
    public void setInterview(Interview interview) { this.interview = interview; }
    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
    public Integer getQuestionOrder() { return questionOrder; }
    public void setQuestionOrder(Integer questionOrder) { this.questionOrder = questionOrder; }
    public String getUserCode() { return userCode; }
    public void setUserCode(String userCode) { this.userCode = userCode; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public Double getMaxScore() { return maxScore; }
    public void setMaxScore(Double maxScore) { this.maxScore = maxScore; }
    public Integer getTestsPassed() { return testsPassed; }
    public void setTestsPassed(Integer testsPassed) { this.testsPassed = testsPassed; }
    public Integer getTestsTotal() { return testsTotal; }
    public void setTestsTotal(Integer testsTotal) { this.testsTotal = testsTotal; }
    public Integer getTimeTakenSeconds() { return timeTakenSeconds; }
    public void setTimeTakenSeconds(Integer timeTakenSeconds) { this.timeTakenSeconds = timeTakenSeconds; }
    public Integer getHintsUsed() { return hintsUsed; }
    public void setHintsUsed(Integer hintsUsed) { this.hintsUsed = hintsUsed; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public String getAiFeedback() { return aiFeedback; }
    public void setAiFeedback(String aiFeedback) { this.aiFeedback = aiFeedback; }
    public Double getCodeQualityScore() { return codeQualityScore; }
    public void setCodeQualityScore(Double codeQualityScore) { this.codeQualityScore = codeQualityScore; }
    public String getComplexityAnalysis() { return complexityAnalysis; }
    public void setComplexityAnalysis(String complexityAnalysis) { this.complexityAnalysis = complexityAnalysis; }
    public String getExecutionOutput() { return executionOutput; }
    public void setExecutionOutput(String executionOutput) { this.executionOutput = executionOutput; }
    public Double getUserEloDelta() { return userEloDelta; }
    public void setUserEloDelta(Double userEloDelta) { this.userEloDelta = userEloDelta; }
    public Double getQuestionEloDelta() { return questionEloDelta; }
    public void setQuestionEloDelta(Double questionEloDelta) { this.questionEloDelta = questionEloDelta; }
}
