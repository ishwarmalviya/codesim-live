package com.codesim.repository;

import com.codesim.entity.UserCompanyTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCompanyTargetRepository extends JpaRepository<UserCompanyTarget, Long> {
    List<UserCompanyTarget> findByUserId(Long userId);
    Optional<UserCompanyTarget> findByUserIdAndCompanyId(Long userId, Long companyId);
    void deleteByUserIdAndCompanyId(Long userId, Long companyId);
}
