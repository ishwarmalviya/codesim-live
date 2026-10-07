package com.codesim.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String difficulty; // easy, medium, hard, expert

    @Column(name = "elo_rating", nullable = false)
    private Double eloRating = 1000.0;

    @Column(nullable = false)
    private String topic;

    private String subtopic = "";

    @Column(name = "function_signature", columnDefinition = "TEXT")
    private String functionSignature = "";

    @Column(name = "starter_code", columnDefinition = "TEXT")
    private String starterCode = "";

    @Column(name = "solution_code", columnDefinition = "TEXT")
    private String solutionCode = "";

    @Column(name = "test_cases", columnDefinition = "TEXT")
    private String testCases = "[]"; // JSON: [{"args": [...], "expected_output": ..., "description": "", "hidden": false}]

    @Column(columnDefinition = "TEXT")
    private String hints = "[]"; // JSON string array

    @Column(columnDefinition = "TEXT")
    private String constraints = "";

    @Column(columnDefinition = "TEXT")
    private String examples = "";

    @Column(name = "time_complexity")
    private String timeComplexity = "";

    @Column(name = "space_complexity")
    private String spaceComplexity = "";

    @Column(length = 300)
    private String tags = "";

    @Column(name = "times_attempted", nullable = false)
    private Integer timesAttempted = 0;

    @Column(name = "times_solved", nullable = false)
    private Integer timesSolved = 0;

    @Column(name = "avg_time_seconds", nullable = false)
    private Double avgTimeSeconds = 0.0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "supported_languages", columnDefinition = "TEXT", nullable = false)
    private String supportedLanguages = "[\"python\"]";

    @Column(name = "starter_code_by_lang", columnDefinition = "TEXT", nullable = false)
    private String starterCodeByLang = "{}";

    public Double getSuccessRate() {
        if (timesAttempted == 0) return 0.0;
        return Math.round((timesSolved / (double) timesAttempted) * 1000) / 10.0;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public Double getEloRating() { return eloRating; }
    public void setEloRating(Double eloRating) { this.eloRating = eloRating; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getSubtopic() { return subtopic; }
    public void setSubtopic(String subtopic) { this.subtopic = subtopic; }
    public String getFunctionSignature() { return functionSignature; }
    public void setFunctionSignature(String functionSignature) { this.functionSignature = functionSignature; }
    public String getStarterCode() { return starterCode; }
    public void setStarterCode(String starterCode) { this.starterCode = starterCode; }
    public String getSolutionCode() { return solutionCode; }
    public void setSolutionCode(String solutionCode) { this.solutionCode = solutionCode; }
    public String getTestCases() { return testCases; }
    public void setTestCases(String testCases) { this.testCases = testCases; }
    public String getHints() { return hints; }
    public void setHints(String hints) { this.hints = hints; }
    public String getConstraints() { return constraints; }
    public void setConstraints(String constraints) { this.constraints = constraints; }
    public String getExamples() { return examples; }
    public void setExamples(String examples) { this.examples = examples; }
    public String getTimeComplexity() { return timeComplexity; }
    public void setTimeComplexity(String timeComplexity) { this.timeComplexity = timeComplexity; }
    public String getSpaceComplexity() { return spaceComplexity; }
    public void setSpaceComplexity(String spaceComplexity) { this.spaceComplexity = spaceComplexity; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public Integer getTimesAttempted() { return timesAttempted; }
    public void setTimesAttempted(Integer timesAttempted) { this.timesAttempted = timesAttempted; }
    public Integer getTimesSolved() { return timesSolved; }
    public void setTimesSolved(Integer timesSolved) { this.timesSolved = timesSolved; }
    public Double getAvgTimeSeconds() { return avgTimeSeconds; }
    public void setAvgTimeSeconds(Double avgTimeSeconds) { this.avgTimeSeconds = avgTimeSeconds; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public String getSupportedLanguages() { return supportedLanguages; }
    public void setSupportedLanguages(String supportedLanguages) { this.supportedLanguages = supportedLanguages; }
    public String getStarterCodeByLang() { return starterCodeByLang; }
    public void setStarterCodeByLang(String starterCodeByLang) { this.starterCodeByLang = starterCodeByLang; }
}
