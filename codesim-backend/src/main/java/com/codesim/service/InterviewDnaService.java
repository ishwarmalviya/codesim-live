package com.codesim.service;

import com.codesim.entity.InterviewDnaProfile;
import com.codesim.entity.InterviewQuestion;
import com.codesim.entity.User;
import com.codesim.repository.InterviewDnaProfileRepository;
import com.codesim.repository.InterviewQuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Derives a behavioral "Interview DNA" profile from a user's InterviewQuestion
 * history: speed, persistence (hint reliance), code quality trend, and
 * favorite topics — then asks the AI engine for a short narrative summary.
 */
@Service
public class InterviewDnaService {

    private static final int TIME_LIMIT_SECONDS = 1800;

    private final InterviewQuestionRepository interviewQuestionRepository;
    private final InterviewDnaProfileRepository dnaProfileRepository;
    private final AiEngineService aiEngineService;
    private final ObjectMapper objectMapper;

    public InterviewDnaService(InterviewQuestionRepository interviewQuestionRepository,
                               InterviewDnaProfileRepository dnaProfileRepository,
                               AiEngineService aiEngineService, ObjectMapper objectMapper) {
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.dnaProfileRepository = dnaProfileRepository;
        this.aiEngineService = aiEngineService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public InterviewDnaProfile recompute(User user) {
        List<InterviewQuestion> history = interviewQuestionRepository.findAllSubmittedByUser(user.getId());

        ObjectNode traits = objectMapper.createObjectNode();

        if (history.isEmpty()) {
            traits.put("status", "insufficient_data");
        } else {
            double avgTimeRatio = history.stream()
                    .mapToDouble(iq -> iq.getTimeTakenSeconds() / (double) TIME_LIMIT_SECONDS)
                    .average().orElse(0.5);
            double avgHints = history.stream().mapToInt(InterviewQuestion::getHintsUsed).average().orElse(0);
            long solvedCount = history.stream().filter(iq -> "solved".equals(iq.getStatus())).count();
            double solveRate = (solvedCount / (double) history.size()) * 100;

            int half = Math.max(1, history.size() / 2);
            double earlyQuality = history.subList(0, half).stream()
                    .mapToDouble(InterviewQuestion::getCodeQualityScore).average().orElse(0);
            double recentQuality = history.subList(history.size() - half, history.size()).stream()
                    .mapToDouble(InterviewQuestion::getCodeQualityScore).average().orElse(0);
            String qualityTrend = recentQuality > earlyQuality + 0.5 ? "improving"
                    : recentQuality < earlyQuality - 0.5 ? "declining" : "stable";

            Map<String, Long> topicCounts = history.stream()
                    .collect(Collectors.groupingBy(iq -> iq.getQuestion().getTopic(), Collectors.counting()));
            List<String> topTopics = topicCounts.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .limit(3).map(Map.Entry::getKey).toList();

            String speedProfile = avgTimeRatio < 0.4 ? "fast" : avgTimeRatio > 0.7 ? "deliberate" : "balanced";
            String persistenceProfile = avgHints < 0.5 ? "independent" : avgHints < 1.5 ? "occasional-hint-user" : "hint-reliant";

            traits.put("speed_profile", speedProfile);
            traits.put("persistence_profile", persistenceProfile);
            traits.put("avg_hints_per_question", Math.round(avgHints * 10) / 10.0);
            traits.put("solve_rate_percent", Math.round(solveRate * 10) / 10.0);
            traits.put("code_quality_trend", qualityTrend);
            traits.put("questions_attempted", history.size());
            traits.set("favorite_topics", objectMapper.valueToTree(topTopics));
        }

        String narrative = aiEngineService.getInterviewDnaNarrative(user.getName(), traits.toString());

        InterviewDnaProfile profile = dnaProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    InterviewDnaProfile p = new InterviewDnaProfile();
                    p.setUser(user);
                    return p;
                });
        profile.setTraits(traits.toString());
        profile.setNarrativeText(narrative);
        profile.setComputedAt(Instant.now());
        return dnaProfileRepository.save(profile);
    }

    @Transactional
    public InterviewDnaProfile getOrCompute(User user) {
        return dnaProfileRepository.findByUserId(user.getId()).orElseGet(() -> recompute(user));
    }
}
