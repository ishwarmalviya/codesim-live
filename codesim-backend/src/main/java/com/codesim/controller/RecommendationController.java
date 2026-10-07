package com.codesim.controller;

import com.codesim.entity.User;
import com.codesim.repository.UserRepository;
import com.codesim.security.UserPrincipal;
import com.codesim.service.RecommendationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserRepository userRepository;

    public RecommendationController(RecommendationService recommendationService, UserRepository userRepository) {
        this.recommendationService = recommendationService;
        this.userRepository = userRepository;
    }

    @GetMapping("/next-question")
    public Map<String, Object> nextQuestion(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        return recommendationService.getNextQuestionRecommendation(user);
    }

    @GetMapping("/questions")
    public List<Map<String, Object>> recommendedQuestions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "false") boolean weakTopicsOnly,
            @RequestParam(defaultValue = "true") boolean ai) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        return recommendationService.getRecommendedQuestions(user, companyId, weakTopicsOnly, ai);
    }
}
