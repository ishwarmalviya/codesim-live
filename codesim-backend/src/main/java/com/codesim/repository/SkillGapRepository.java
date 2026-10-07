package com.codesim.repository;

import com.codesim.entity.SkillGap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillGapRepository extends JpaRepository<SkillGap, Long> {
    List<SkillGap> findByUserIdOrderByGapSeverityDesc(Long userId);
    void deleteByUserId(Long userId);
}
