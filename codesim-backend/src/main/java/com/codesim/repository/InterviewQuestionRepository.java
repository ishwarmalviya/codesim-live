package com.codesim.repository;

import com.codesim.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
    List<InterviewQuestion> findByInterviewIdOrderByQuestionOrderAsc(Long interviewId);

    @Query("SELECT iq FROM InterviewQuestion iq WHERE iq.interview.user.id = :userId " +
           "AND iq.submittedAt IS NOT NULL ORDER BY iq.submittedAt ASC")
    List<InterviewQuestion> findAllSubmittedByUser(Long userId);
}
