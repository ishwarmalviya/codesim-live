package com.codesim.service;

import com.codesim.dto.QuestionDto;
import com.codesim.entity.*;
import com.codesim.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final AdaptiveEngineService adaptiveEngineService;
    private final SkillIntelligenceService skillIntelligenceService;
    private final TopicPerformanceRepository topicPerformanceRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final QuestionRepository questionRepository;
    private final CompanyRepository companyRepository;
    private final AiEngineService aiEngineService;
    private final InterviewService interviewService;
    private final ObjectMapper objectMapper;

    public RecommendationService(AdaptiveEngineService adaptiveEngineService,
                                  SkillIntelligenceService skillIntelligenceService,
                                  TopicPerformanceRepository topicPerformanceRepository,
                                  InterviewQuestionRepository interviewQuestionRepository,
                                  QuestionRepository questionRepository,
                                  CompanyRepository companyRepository,
                                  AiEngineService aiEngineService,
                                  InterviewService interviewService,
                                  ObjectMapper objectMapper) {
        this.adaptiveEngineService = adaptiveEngineService;
        this.skillIntelligenceService = skillIntelligenceService;
        this.topicPerformanceRepository = topicPerformanceRepository;
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.questionRepository = questionRepository;
        this.companyRepository = companyRepository;
        this.aiEngineService = aiEngineService;
        this.interviewService = interviewService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getNextQuestionRecommendation(User user) {
        List<Long> answeredIds = interviewQuestionRepository.findAllSubmittedByUser(user.getId()).stream()
                .map(iq -> iq.getQuestion().getId()).distinct().collect(Collectors.toList());

        var targets = skillIntelligenceService.getTargets(user.getId());
        Double companyElo = null;
        if (!targets.isEmpty()) {
            companyElo = targets.get(0).getCompany().getDifficultyThreshold();
        }

        var questionOpt = adaptiveEngineService.selectNextQuestion(user, answeredIds, "adaptive", null, companyElo);

        Map<String, Object> result = new LinkedHashMap<>();
        if (questionOpt.isEmpty()) {
            result.put("available", false);
            return result;
        }

        Question q = questionOpt.get();
        QuestionDto dto = interviewService.toQuestionDto(q);

        Map<String, Double> prof = topicPerformanceRepository.findByUserId(user.getId()).stream()
                .collect(Collectors.toMap(TopicPerformance::getTopic, TopicPerformance::getProficiency));

        String reason = buildReason(q, prof, targets);
        if (aiEngineService.hasApiKey()) {
            try {
                ObjectNode ctx = objectMapper.createObjectNode();
                ctx.put("userElo", user.getEloRating());
                ctx.put("weakestTopic", prof.entrySet().stream()
                        .min(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("Arrays"));
                ctx.put("recommendedTitle", q.getTitle());
                ctx.put("recommendedTopic", q.getTopic());
                var aiReason = aiEngineService.suggestNextQuestion(ctx.toString());
                if (aiReason.has("reason")) reason = aiReason.get("reason").asText(reason);
            } catch (Exception ignored) {
            }
        }

        result.put("available", true);
        result.put("questionId", q.getId());
        result.put("title", q.getTitle());
        result.put("topic", q.getTopic());
        result.put("difficulty", q.getDifficulty());
        result.put("reason", reason);
        result.put("question", dto);
        return result;
    }

    private String buildReason(Question q, Map<String, Double> prof, List<UserCompanyTarget> targets) {
        double topicProf = prof.getOrDefault(q.getTopic(), 0.0);
        if (topicProf < 40) {
            return "Your " + q.getTopic() + " proficiency is " + Math.round(topicProf) + "% — this problem will help you improve.";
        }
        if (!targets.isEmpty()) {
            return "Recommended for " + targets.get(0).getCompany().getName() + " prep at your current Elo.";
        }
        return "Matched to your Elo (" + Math.round(q.getEloRating()) + ") for optimal challenge.";
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecommendedQuestions(User user, Long companyId,
                                                              boolean weakTopicsOnly, boolean ai) {
        List<Long> solvedIds = interviewQuestionRepository.findAllSubmittedByUser(user.getId()).stream()
                .filter(iq -> "solved".equals(iq.getStatus()))
                .map(iq -> iq.getQuestion().getId()).distinct().collect(Collectors.toList());

        Map<String, Double> prof = topicPerformanceRepository.findByUserId(user.getId()).stream()
                .collect(Collectors.toMap(TopicPerformance::getTopic, TopicPerformance::getProficiency));

        List<String> weakTopics = prof.entrySet().stream()
                .filter(e -> e.getValue() < 40)
                .map(Map.Entry::getKey)
                .toList();

        Double companyElo = null;
        List<String> companyTopics = new ArrayList<>();
        if (companyId != null) {
            Company company = companyRepository.findById(companyId).orElse(null);
            if (company != null) {
                companyElo = company.getDifficultyThreshold();
                try {
                    var weights = objectMapper.readTree(company.getRequiredTopics());
                    weights.fieldNames().forEachRemaining(companyTopics::add);
                } catch (Exception ignored) {
                }
            }
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String topicFilter = weakTopicsOnly && !weakTopics.isEmpty()
                    ? weakTopics.get(i % weakTopics.size()) : "adaptive";
            var qOpt = adaptiveEngineService.selectNextQuestionWithFallback(
                    user, solvedIds, topicFilter, companyTopics.isEmpty() ? null : companyTopics, companyElo);
            if (qOpt.isEmpty()) break;
            Question q = qOpt.get();
            if (solvedIds.contains(q.getId())) continue;
            solvedIds.add(q.getId());

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("questionId", q.getId());
            item.put("title", q.getTitle());
            item.put("topic", q.getTopic());
            item.put("difficulty", q.getDifficulty());
            item.put("reason", buildReason(q, prof, skillIntelligenceService.getTargets(user.getId())));
            results.add(item);
        }
        return results;
    }
}
