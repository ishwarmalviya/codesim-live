package com.codesim.repository;

import com.codesim.entity.InterviewReplayEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewReplayEventRepository extends JpaRepository<InterviewReplayEvent, Long> {
    List<InterviewReplayEvent> findByInterviewQuestionIdOrderByCreatedAtAsc(Long interviewQuestionId);
}
