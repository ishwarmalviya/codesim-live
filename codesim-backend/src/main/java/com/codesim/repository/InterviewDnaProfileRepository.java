package com.codesim.repository;

import com.codesim.entity.InterviewDnaProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InterviewDnaProfileRepository extends JpaRepository<InterviewDnaProfile, Long> {
    Optional<InterviewDnaProfile> findByUserId(Long userId);
}
