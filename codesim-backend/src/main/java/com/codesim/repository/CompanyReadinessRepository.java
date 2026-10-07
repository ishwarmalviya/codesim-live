package com.codesim.repository;

import com.codesim.entity.CompanyReadiness;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyReadinessRepository extends JpaRepository<CompanyReadiness, Long> {
    List<CompanyReadiness> findByUserId(Long userId);
    Optional<CompanyReadiness> findByUserIdAndCompanyId(Long userId, Long companyId);
}
