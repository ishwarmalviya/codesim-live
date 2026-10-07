package com.codesim.service;

import com.codesim.entity.InterviewQuestion;
import com.codesim.entity.InterviewReplayEvent;
import com.codesim.repository.InterviewReplayEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Records code snapshots / run / submit / hint events during an interview
 * question attempt, powering the Interview Replay Analytics timeline.
 */
@Service
public class ReplayService {

    private final InterviewReplayEventRepository repository;
    private final ObjectMapper objectMapper;

    public ReplayService(InterviewReplayEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void recordEvent(InterviewQuestion iq, String eventType, String code, Object payload) {
        InterviewReplayEvent event = new InterviewReplayEvent();
        event.setInterviewQuestion(iq);
        event.setEventType(eventType);
        event.setCodeSnapshot(code == null ? "" : code);
        try {
            event.setPayload(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            event.setPayload("{}");
        }
        repository.save(event);
    }

    public List<InterviewReplayEvent> getTimeline(Long interviewQuestionId) {
        return repository.findByInterviewQuestionIdOrderByCreatedAtAsc(interviewQuestionId);
    }
}
