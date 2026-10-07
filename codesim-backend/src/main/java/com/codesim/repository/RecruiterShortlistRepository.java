package com.codesim.repository;

import com.codesim.entity.RecruiterShortlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecruiterShortlistRepository extends JpaRepository<RecruiterShortlist, Long> {
    List<RecruiterShortlist> findByRecruiterIdOrderByAddedAtDesc(Long recruiterId);
    Optional<RecruiterShortlist> findByRecruiterIdAndCandidateId(Long recruiterId, Long candidateId);
    void deleteByRecruiterIdAndCandidateId(Long recruiterId, Long candidateId);
}
