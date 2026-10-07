package com.codesim.repository;

import com.codesim.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
    List<Interview> findByUserIdAndStatusOrderByCompletedAtDesc(Long userId, String status);
    long countByStatus(String status);
    long countByStatusAndCompletedAtGreaterThanEqual(String status, Instant since);
    long countByUserIdAndStatus(Long userId, String status);
    long countByStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(String status, Instant from, Instant to);
    List<Interview> findByStatusAndMaxScoreGreaterThan(String status, Double maxScore);

    @Query("SELECT AVG(i.score / i.maxScore * 100) FROM Interview i WHERE i.status = :status AND i.maxScore > 0")
    Double avgScorePercent(String status);
}
