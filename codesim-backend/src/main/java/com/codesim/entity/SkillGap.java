package com.codesim.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "skill_gaps")
public class SkillGap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false)
    private String topic;

    @Column(name = "required_proficiency", nullable = false)
    private Double requiredProficiency = 0.0;

    @Column(name = "actual_proficiency", nullable = false)
    private Double actualProficiency = 0.0;

    @Column(name = "gap_severity", nullable = false)
    private Double gapSeverity = 0.0;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public Double getRequiredProficiency() { return requiredProficiency; }
    public void setRequiredProficiency(Double requiredProficiency) { this.requiredProficiency = requiredProficiency; }
    public Double getActualProficiency() { return actualProficiency; }
    public void setActualProficiency(Double actualProficiency) { this.actualProficiency = actualProficiency; }
    public Double getGapSeverity() { return gapSeverity; }
    public void setGapSeverity(Double gapSeverity) { this.gapSeverity = gapSeverity; }
    public Instant getComputedAt() { return computedAt; }
    public void setComputedAt(Instant computedAt) { this.computedAt = computedAt; }
}
