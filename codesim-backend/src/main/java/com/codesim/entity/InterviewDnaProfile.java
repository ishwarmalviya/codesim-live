package com.codesim.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "interview_dna_profiles")
public class InterviewDnaProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String traits = "{}";

    @Column(name = "narrative_text", nullable = false, columnDefinition = "TEXT")
    private String narrativeText = "";

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getTraits() { return traits; }
    public void setTraits(String traits) { this.traits = traits; }
    public String getNarrativeText() { return narrativeText; }
    public void setNarrativeText(String narrativeText) { this.narrativeText = narrativeText; }
    public Instant getComputedAt() { return computedAt; }
    public void setComputedAt(Instant computedAt) { this.computedAt = computedAt; }
}
