package com.codesim.controller;

import com.codesim.dto.PhaseTwoDtos.*;
import com.codesim.entity.RoadmapItem;
import com.codesim.entity.User;
import com.codesim.repository.ResumeRepository;
import com.codesim.repository.RoadmapItemRepository;
import com.codesim.repository.UserRepository;
import com.codesim.security.UserPrincipal;
import com.codesim.service.AiEngineService;
import com.codesim.service.SkillIntelligenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SkillIntelligenceController {

    private final SkillIntelligenceService skillIntelligenceService;
    private final AiEngineService aiEngineService;
    private final ResumeRepository resumeRepository;
    private final RoadmapItemRepository roadmapItemRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public SkillIntelligenceController(SkillIntelligenceService skillIntelligenceService,
                                          AiEngineService aiEngineService,
                                          ResumeRepository resumeRepository,
                                          RoadmapItemRepository roadmapItemRepository,
                                          UserRepository userRepository,
                                          ObjectMapper objectMapper) {
        this.skillIntelligenceService = skillIntelligenceService;
        this.aiEngineService = aiEngineService;
        this.resumeRepository = resumeRepository;
        this.roadmapItemRepository = roadmapItemRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    private User currentUser(UserPrincipal principal) {
        return userRepository.findById(principal.getId()).orElseThrow();
    }

    @GetMapping("/skill-gaps")
    public List<SkillGapDto> skillGaps(@AuthenticationPrincipal UserPrincipal principal) {
        User user = currentUser(principal);
        return skillIntelligenceService.getSkillGaps(user.getId()).stream().map(g -> {
            SkillGapDto dto = new SkillGapDto();
            dto.topic = g.getTopic();
            dto.requiredProficiency = g.getRequiredProficiency();
            dto.actualProficiency = g.getActualProficiency();
            dto.gapSeverity = g.getGapSeverity();
            return dto;
        }).toList();
    }

    @GetMapping("/roadmap")
    @Transactional(readOnly = true)
    public List<RoadmapItemDto> roadmap(@AuthenticationPrincipal UserPrincipal principal) {
        User user = currentUser(principal);
        return skillIntelligenceService.getRoadmap(user.getId()).stream().map(item -> {
            RoadmapItemDto dto = new RoadmapItemDto();
            dto.id = item.getId();
            dto.topic = item.getTopic();
            dto.questionId = item.getQuestion() != null ? item.getQuestion().getId() : null;
            dto.questionTitle = item.getQuestion() != null ? item.getQuestion().getTitle() : null;
            dto.priorityRank = item.getPriorityRank();
            dto.status = item.getStatus();
            dto.reason = item.getReason();
            return dto;
        }).toList();
    }

    @PostMapping("/roadmap/generate")
    @Transactional
    public com.fasterxml.jackson.databind.JsonNode generateRoadmap(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody RoadmapGenerateRequest req) {
        User user = currentUser(principal);
        ObjectNode profile = objectMapper.createObjectNode();
        profile.put("skillLevel", req.skillLevel != null ? req.skillLevel : "Intermediate");
        profile.put("placementGoal", req.placementGoal != null ? req.placementGoal : "full-time");
        if (req.useSavedResume) {
            resumeRepository.findByUserId(user.getId()).ifPresent(r -> profile.put("resume", r.getRawText()));
        } else if (req.resumeText != null) {
            profile.put("resume", req.resumeText);
        }
        var companies = objectMapper.createArrayNode();
        if (req.targetCompanyIds != null) {
            req.targetCompanyIds.forEach(id -> companies.add(id));
        }
        profile.set("targetCompanies", companies);

        var roadmap = aiEngineService.generatePersonalizedRoadmap(profile.toString());

        int rank = 1;
        if (roadmap.has("phases")) {
            for (var phase : roadmap.get("phases")) {
                RoadmapItem item = new RoadmapItem();
                item.setUser(user);
                item.setTopic(phase.path("focus").asText("General"));
                item.setPriorityRank(rank++);
                item.setStatus("pending");
                item.setReason(phase.path("goals").isArray() && phase.get("goals").size() > 0
                        ? phase.get("goals").get(0).asText() : "AI-generated milestone");
                item.setSource("ai_generated");
                item.setUpdatedAt(Instant.now());
                roadmapItemRepository.save(item);
            }
        }
        return roadmap;
    }
}
