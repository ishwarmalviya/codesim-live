package com.codesim.repository;

import com.codesim.entity.PlacementScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PlacementScoreRepository extends JpaRepository<PlacementScore, Long> {
    Optional<PlacementScore> findByUserId(Long userId);

    @Query("SELECT p FROM PlacementScore p WHERE p.user.role = 'USER' " +
           "AND (:minScore IS NULL OR p.overallScore >= :minScore) " +
           "ORDER BY p.overallScore DESC")
    Page<PlacementScore> searchCandidates(Double minScore, Pageable pageable);
}
