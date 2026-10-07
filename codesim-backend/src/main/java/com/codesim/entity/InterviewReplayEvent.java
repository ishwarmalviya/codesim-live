package com.codesim.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "interview_replay_events")
public class InterviewReplayEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_question_id", nullable = false)
    private InterviewQuestion interviewQuestion;

    @Column(name = "event_type", nullable = false)
    private String eventType; // run, submit, hint

    @Column(name = "code_snapshot", columnDefinition = "TEXT")
    private String codeSnapshot = "";

    @Column(columnDefinition = "TEXT")
    private String payload = "{}";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public InterviewQuestion getInterviewQuestion() { return interviewQuestion; }
    public void setInterviewQuestion(InterviewQuestion interviewQuestion) { this.interviewQuestion = interviewQuestion; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getCodeSnapshot() { return codeSnapshot; }
    public void setCodeSnapshot(String codeSnapshot) { this.codeSnapshot = codeSnapshot; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
