package com.codesim.repository;

import com.codesim.entity.CompanyRoadmap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRoadmapRepository extends JpaRepository<CompanyRoadmap, Long> {
    Optional<CompanyRoadmap> findByUserIdAndCompanyId(Long userId, Long companyId);
    List<CompanyRoadmap> findByUserId(Long userId);
}
