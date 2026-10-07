package com.codesim.service;

import com.codesim.entity.CompanyReadiness;
import com.codesim.entity.Interview;
import com.codesim.entity.InterviewQuestion;
import com.codesim.entity.PlacementScore;
import com.codesim.entity.User;
import com.codesim.repository.InterviewQuestionRepository;
import com.codesim.repository.InterviewRepository;
import com.codesim.repository.PlacementScoreRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Combines Elo, average company readiness, score consistency across
 * interviews, and code-quality trend into a single "placement readiness"
 * index — the Placement Intelligence Engine.
 */
@Service
public class PlacementIntelligenceService {

    private final PlacementScoreRepository placementScoreRepository;
    private final InterviewRepository interviewRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final SkillIntelligenceService skillIntelligenceService;
    private final ObjectMapper objectMapper;

    public PlacementIntelligenceService(PlacementScoreRepository placementScoreRepository,
                                         InterviewRepository interviewRepository,
                                         InterviewQuestionRepository interviewQuestionRepository,
                                         SkillIntelligenceService skillIntelligenceService,
                                         ObjectMapper objectMapper) {
        this.placementScoreRepository = placementScoreRepository;
        this.interviewRepository = interviewRepository;
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.skillIntelligenceService = skillIntelligenceService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PlacementScore recompute(User user) {
        double eloComponent = Math.min(100, Math.max(0, ((user.getEloRating() - 400) / (2000 - 400)) * 100));

        List<CompanyReadiness> readinessList = skillIntelligenceService.getReadiness(user.getId());
        double readinessComponent = readinessList.isEmpty() ? 20.0
                : readinessList.stream().mapToDouble(CompanyReadiness::getReadinessScore).average().orElse(20.0);

        List<Interview> completed = interviewRepository.findByUserIdAndStatusOrderByCompletedAtDesc(user.getId(), "completed");
        double consistencyComponent = 30.0;
        if (completed.size() >= 2) {
            double mean = completed.stream().mapToDouble(Interview::getScorePercent).average().orElse(0);
            double variance = completed.stream()
                    .mapToDouble(i -> Math.pow(i.getScorePercent() - mean, 2)).average().orElse(0);
            double stddev = Math.sqrt(variance);
            consistencyComponent = Math.max(0, 100 - stddev);
        }

        List<InterviewQuestion> history = interviewQuestionRepository.findAllSubmittedByUser(user.getId());
        int questionsAttempted = history.size();
        double qualityComponent = history.isEmpty() ? 50.0
                : history.stream().mapToDouble(InterviewQuestion::getCodeQualityScore).average().orElse(5.0) * 10;

        double overall = 0.35 * eloComponent + 0.25 * readinessComponent + 0.2 * consistencyComponent + 0.2 * qualityComponent;

        if (questionsAttempted < 10) {
            overall = overall * (questionsAttempted / 10.0);
        }

        ObjectNode breakdown = objectMapper.createObjectNode();
        breakdown.put("eloComponent", Math.round(eloComponent * 10) / 10.0);
        breakdown.put("readinessComponent", Math.round(readinessComponent * 10) / 10.0);
        breakdown.put("consistencyComponent", Math.round(consistencyComponent * 10) / 10.0);
        breakdown.put("qualityComponent", Math.round(qualityComponent * 10) / 10.0);
        breakdown.put("interviewsCompleted", completed.size());
        breakdown.put("questionsAttempted", questionsAttempted);
        breakdown.put("confidence", questionsAttempted < 10 ? "low" : questionsAttempted < 25 ? "medium" : "high");
        breakdown.put("formula", "35% Elo + 25% Company Readiness + 20% Consistency + 20% Code Quality");

        PlacementScore score = placementScoreRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    PlacementScore s = new PlacementScore();
                    s.setUser(user);
                    return s;
                });
        score.setOverallScore(Math.round(overall * 10) / 10.0);
        score.setBreakdown(breakdown.toString());
        score.setComputedAt(Instant.now());
        return placementScoreRepository.save(score);
    }

    @Transactional
    public PlacementScore getOrCompute(User user) {
        return placementScoreRepository.findByUserId(user.getId()).orElseGet(() -> recompute(user));
    }
}
