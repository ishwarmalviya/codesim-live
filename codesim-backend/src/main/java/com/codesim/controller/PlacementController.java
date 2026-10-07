package com.codesim.controller;

import com.codesim.dto.PhaseThreeDtos.PlacementScoreDto;
import com.codesim.entity.PlacementScore;
import com.codesim.entity.User;
import com.codesim.repository.UserRepository;
import com.codesim.security.UserPrincipal;
import com.codesim.service.AiEngineService;
import com.codesim.service.PlacementIntelligenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/placement-score")
public class PlacementController {

    private final PlacementIntelligenceService placementIntelligenceService;
    private final AiEngineService aiEngineService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public PlacementController(PlacementIntelligenceService placementIntelligenceService,
                                AiEngineService aiEngineService,
                                UserRepository userRepository,
                                ObjectMapper objectMapper) {
        this.placementIntelligenceService = placementIntelligenceService;
        this.aiEngineService = aiEngineService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public PlacementScoreDto get(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        PlacementScore score = placementIntelligenceService.getOrCompute(user);
        PlacementScoreDto dto = new PlacementScoreDto();
        dto.overallScore = score.getOverallScore();
        dto.computedAt = score.getComputedAt();
        try {
            dto.breakdown = objectMapper.readTree(score.getBreakdown());
        } catch (Exception e) {
            dto.breakdown = objectMapper.createObjectNode();
        }
        return dto;
    }

    @GetMapping("/improvements")
    public com.fasterxml.jackson.databind.JsonNode improvements(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        PlacementScore score = placementIntelligenceService.getOrCompute(user);
        return aiEngineService.getPlacementImprovements(score.getBreakdown(), user.getName());
    }
}
