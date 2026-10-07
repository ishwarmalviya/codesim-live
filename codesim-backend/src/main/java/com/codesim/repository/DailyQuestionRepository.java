package com.codesim.repository;

import com.codesim.entity.DailyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DailyQuestionRepository extends JpaRepository<DailyQuestion, Long> {

    @Query("SELECT dq FROM DailyQuestion dq WHERE dq.expiresAt > :now ORDER BY dq.assignedAt DESC")
    List<DailyQuestion> findActive(Instant now);

    default Optional<DailyQuestion> findCurrent(Instant now) {
        List<DailyQuestion> active = findActive(now);
        return active.isEmpty() ? Optional.empty() : Optional.of(active.get(0));
    }
}
