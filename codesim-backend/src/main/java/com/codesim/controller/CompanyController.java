package com.codesim.controller;

import com.codesim.dto.PhaseTwoDtos.*;
import com.codesim.entity.Company;
import com.codesim.entity.CompanyReadiness;
import com.codesim.entity.User;
import com.codesim.repository.UserRepository;
import com.codesim.security.UserPrincipal;
import com.codesim.service.SkillIntelligenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final SkillIntelligenceService skillIntelligenceService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public CompanyController(SkillIntelligenceService skillIntelligenceService, UserRepository userRepository,
                              ObjectMapper objectMapper) {
        this.skillIntelligenceService = skillIntelligenceService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    private User currentUser(UserPrincipal principal) {
        return userRepository.findById(principal.getId()).orElseThrow();
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<CompanyDto> list(@AuthenticationPrincipal UserPrincipal principal) {
        User user = currentUser(principal);
        var targetedIds = skillIntelligenceService.getTargets(user.getId()).stream()
                .map(t -> t.getCompany().getId()).collect(Collectors.toSet());
        var readinessMap = skillIntelligenceService.getReadiness(user.getId()).stream()
                .collect(Collectors.toMap(r -> r.getCompany().getId(), CompanyReadiness::getReadinessScore));

        return skillIntelligenceService.listCompanies().stream().map(c -> {
            CompanyDto dto = toDto(c);
            dto.targeted = targetedIds.contains(c.getId());
            dto.readinessScore = readinessMap.get(c.getId());
            return dto;
        }).toList();
    }

    @PostMapping("/{id}/target")
    public ResponseEntity<Void> addTarget(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        skillIntelligenceService.addTarget(currentUser(principal), id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/target")
    public ResponseEntity<Void> removeTarget(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        skillIntelligenceService.removeTarget(currentUser(principal), id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/readiness")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> readiness(@AuthenticationPrincipal UserPrincipal principal) {
        User user = currentUser(principal);
        return skillIntelligenceService.getReadiness(user.getId()).stream().map(r -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("companyId", r.getCompany().getId());
            m.put("companyName", r.getCompany().getName());
            m.put("logoEmoji", r.getCompany().getLogoEmoji());
            m.put("readinessScore", r.getReadinessScore());
            try {
                m.put("breakdown", objectMapper.readTree(r.getBreakdown()));
            } catch (Exception e) {
                m.put("breakdown", null);
            }
            return m;
        }).toList();
    }

    @GetMapping("/{id}/roadmap")
    @Transactional(readOnly = true)
    public Map<String, Object> companyRoadmap(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        User user = currentUser(principal);
        return skillIntelligenceService.getCompanyRoadmapMerged(user.getId(), id);
    }

    private CompanyDto toDto(Company c) {
        CompanyDto dto = new CompanyDto();
        dto.id = c.getId();
        dto.name = c.getName();
        dto.logoEmoji = c.getLogoEmoji();
        dto.difficultyThreshold = c.getDifficultyThreshold();
        dto.tags = c.getTags();
        try {
            dto.requiredTopics = objectMapper.readTree(c.getRequiredTopics());
        } catch (Exception e) {
            dto.requiredTopics = objectMapper.createObjectNode();
        }
        return dto;
    }
}
