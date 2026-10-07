package com.codesim.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "logo_emoji", length = 10)
    private String logoEmoji = "🏢";

    @Column(name = "required_topics", nullable = false, columnDefinition = "TEXT")
    private String requiredTopics = "{}"; // JSON: {"Arrays": 0.8, "Trees": 0.6, ...} weights 0-1

    @Column(name = "difficulty_threshold", nullable = false)
    private Double difficultyThreshold = 1200.0;

    @Column(length = 300)
    private String tags = "";

    @Column(name = "roadmap_template", columnDefinition = "TEXT")
    private String roadmapTemplate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLogoEmoji() { return logoEmoji; }
    public void setLogoEmoji(String logoEmoji) { this.logoEmoji = logoEmoji; }
    public String getRequiredTopics() { return requiredTopics; }
    public void setRequiredTopics(String requiredTopics) { this.requiredTopics = requiredTopics; }
    public Double getDifficultyThreshold() { return difficultyThreshold; }
    public void setDifficultyThreshold(Double difficultyThreshold) { this.difficultyThreshold = difficultyThreshold; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getRoadmapTemplate() { return roadmapTemplate; }
    public void setRoadmapTemplate(String roadmapTemplate) { this.roadmapTemplate = roadmapTemplate; }
}
