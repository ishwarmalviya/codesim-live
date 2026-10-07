package com.codesim.repository;

import com.codesim.entity.TopicPerformance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TopicPerformanceRepository extends JpaRepository<TopicPerformance, Long> {
    List<TopicPerformance> findByUserIdOrderByProficiencyAsc(Long userId);
    List<TopicPerformance> findByUserId(Long userId);
    Optional<TopicPerformance> findByUserIdAndTopic(Long userId, String topic);
}
