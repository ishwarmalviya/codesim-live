package com.codesim.repository;

import com.codesim.entity.RoadmapItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoadmapItemRepository extends JpaRepository<RoadmapItem, Long> {
    List<RoadmapItem> findByUserIdOrderByPriorityRankAsc(Long userId);
    void deleteByUserIdAndStatus(Long userId, String status);
}
