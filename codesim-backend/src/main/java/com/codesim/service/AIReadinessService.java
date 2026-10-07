package com.codesim.service;

import com.codesim.entity.*;
import com.codesim.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AIReadinessService {

    private final TopicPerformanceRepository topicPerformanceRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final RoadmapItemRepository roadmapItemRepository;
    private final InterviewRepository interviewRepository;

    public AIReadinessService(TopicPerformanceRepository topicPerformanceRepository,
                               InterviewQuestionRepository interviewQuestionRepository,
                               RoadmapItemRepository roadmapItemRepository,
                               InterviewRepository interviewRepository) {
        this.topicPerformanceRepository = topicPerformanceRepository;
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.roadmapItemRepository = roadmapItemRepository;
        this.interviewRepository = interviewRepository;
    }

    public record ReadinessFactors(
            double eloScore,
            double topicCoverage,
            double difficultyProgress,
            double accuracy,
            double timeEfficiency,
            double consistency,
            double roadmapCompletion,
            double performanceTrend,
            int questionsAttempted,
            String confidence
    ) {}

    public ReadinessFactors computeFactors(User user, Company company, Map<String, Double> weights,
                                            Map<String, Double> proficiency) {
        double weightSum = weights.values().stream().mapToDouble(Double::doubleValue).sum();

        double topicCoverage = 0;
        if (weightSum > 0) {
            double weighted = 0;
            double counted = 0;
            for (var e : weights.entrySet()) {
                TopicPerformance tp = topicPerformanceRepository.findByUserIdAndTopic(user.getId(), e.getKey()).orElse(null);
                double actual = (tp != null && tp.getQuestionsAttempted() >= 3)
                        ? proficiency.getOrDefault(e.getKey(), 0.0) : 0.0;
                weighted += e.getValue() * actual;
                counted += e.getValue();
            }
            topicCoverage = counted > 0 ? weighted / counted : 0;
        }

        double eloScore = Math.min(100, Math.max(0, (user.getEloRating() / company.getDifficultyThreshold()) * 100));

        List<InterviewQuestion> history = interviewQuestionRepository.findAllSubmittedByUser(user.getId());
        int questionsAttempted = history.size();

        Set<String> companyTopics = weights.keySet();
        List<InterviewQuestion> companyTopicHistory = history.stream()
                .filter(iq -> companyTopics.contains(iq.getQuestion().getTopic()))
                .toList();

        double accuracy = companyTopicHistory.isEmpty() ? 0
                : companyTopicHistory.stream().filter(iq -> "solved".equals(iq.getStatus())).count()
                / (double) companyTopicHistory.size() * 100;

        double difficultyProgress = companyTopicHistory.stream()
                .filter(iq -> "solved".equals(iq.getStatus()))
                .mapToDouble(iq -> switch (iq.getQuestion().getDifficulty()) {
                    case "easy" -> 25;
                    case "medium" -> 50;
                    case "hard" -> 75;
                    case "expert" -> 100;
                    default -> 40;
                }).average().orElse(0);

        double timeEfficiency = companyTopicHistory.stream()
                .filter(iq -> "solved".equals(iq.getStatus()) && iq.getTimeTakenSeconds() > 0)
                .mapToDouble(iq -> Math.min(100, (1.0 - iq.getTimeTakenSeconds() / 1800.0) * 100))
                .average().orElse(50);

        List<Interview> completed = interviewRepository.findByUserIdAndStatusOrderByCompletedAtDesc(user.getId(), "completed");
        double consistency = 50;
        if (completed.size() >= 2) {
            double mean = completed.stream().mapToDouble(Interview::getScorePercent).average().orElse(0);
            double variance = completed.stream()
                    .mapToDouble(i -> Math.pow(i.getScorePercent() - mean, 2)).average().orElse(0);
            consistency = Math.max(0, 100 - Math.sqrt(variance));
        }

        List<RoadmapItem> roadmap = roadmapItemRepository.findByUserIdOrderByPriorityRankAsc(user.getId());
        double roadmapCompletion = roadmap.isEmpty() ? 50
                : roadmap.stream().filter(r -> "completed".equals(r.getStatus())).count()
                / (double) roadmap.size() * 100;

        double performanceTrend = 50;
        if (history.size() >= 3) {
            List<InterviewQuestion> recent = history.subList(Math.max(0, history.size() - 5), history.size());
            double firstHalf = recent.subList(0, recent.size() / 2).stream()
                    .mapToDouble(InterviewQuestion::getScore).average().orElse(0);
            double secondHalf = recent.subList(recent.size() / 2, recent.size()).stream()
                    .mapToDouble(InterviewQuestion::getScore).average().orElse(0);
            performanceTrend = Math.min(100, Math.max(0, 50 + (secondHalf - firstHalf) * 10));
        }

        String confidence = questionsAttempted < 10 ? "low" : questionsAttempted < 25 ? "medium" : "high";

        return new ReadinessFactors(
                eloScore, topicCoverage, difficultyProgress, accuracy,
                timeEfficiency, consistency, roadmapCompletion, performanceTrend,
                questionsAttempted, confidence
        );
    }

    public double computeOverallScore(ReadinessFactors f) {
        double raw = 0.15 * f.eloScore()
                + 0.25 * f.topicCoverage()
                + 0.15 * f.difficultyProgress()
                + 0.15 * f.accuracy()
                + 0.10 * f.timeEfficiency()
                + 0.05 * f.consistency()
                + 0.10 * f.roadmapCompletion()
                + 0.05 * f.performanceTrend();

        if (f.questionsAttempted() < 15) {
            double cap = 30 + f.questionsAttempted() * 3.0;
            raw = Math.min(raw, cap);
        }
        return Math.round(raw * 10) / 10.0;
    }
}
