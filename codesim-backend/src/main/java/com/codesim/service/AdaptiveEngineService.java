package com.codesim.service;

import com.codesim.entity.Question;
import com.codesim.entity.TopicPerformance;
import com.codesim.entity.User;
import com.codesim.repository.QuestionRepository;
import com.codesim.repository.TopicPerformanceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class AdaptiveEngineService {

    private static final double CHALLENGE_OFFSET = 75;
    private static final double SELECTION_WINDOW = 200;
    private static final double TOPIC_WEIGHT_FACTOR = 1.5;

    private final QuestionRepository questionRepository;
    private final TopicPerformanceRepository topicPerformanceRepository;

    public AdaptiveEngineService(QuestionRepository questionRepository,
                                  TopicPerformanceRepository topicPerformanceRepository) {
        this.questionRepository = questionRepository;
        this.topicPerformanceRepository = topicPerformanceRepository;
    }

    public Optional<Question> selectNextQuestion(User user, List<Long> answeredIds, String topicFilter) {
        return selectNextQuestion(user, answeredIds, topicFilter, null, null);
    }

    public Optional<Question> selectNextQuestion(User user, List<Long> answeredIds, String topicFilter,
                                                  List<String> topicList, Double companyEloTarget) {
        double challengeOffset = CHALLENGE_OFFSET;
        if (companyEloTarget != null) {
            challengeOffset = Math.max(0, companyEloTarget - user.getEloRating()) * 0.5 + 50;
        }
        double targetElo = user.getEloRating() + challengeOffset;
        double minElo = targetElo - SELECTION_WINDOW;
        double maxElo = targetElo + SELECTION_WINDOW;
        String topic = (topicFilter == null || topicFilter.equals("adaptive")) ? null : topicFilter;

        List<Long> excluded = answeredIds.isEmpty() ? List.of(-1L) : answeredIds;

        List<Question> candidates = questionRepository.findCandidates(excluded, minElo, maxElo, topic);

        if (candidates.isEmpty()) {
            candidates = questionRepository.findBroadCandidates(excluded, topic, targetElo, PageRequest.of(0, 10));
        }

        if (topicList != null && !topicList.isEmpty()) {
            candidates = candidates.stream()
                    .filter(q -> topicList.contains(q.getTopic()))
                    .collect(Collectors.toList());
            if (candidates.isEmpty()) {
                candidates = questionRepository.findBroadCandidates(excluded, null, targetElo, PageRequest.of(0, 20))
                        .stream().filter(q -> topicList.contains(q.getTopic())).collect(Collectors.toList());
            }
        }

        if (candidates.isEmpty()) return Optional.empty();

        return Optional.ofNullable(pickBest(candidates, targetElo, user));
    }

    /**
     * Progressive fallback: never return empty while active questions exist in the bank.
     */
    public Optional<Question> selectNextQuestionWithFallback(User user, List<Long> answeredIds,
                                                              String topicFilter, List<String> topicList,
                                                              Double companyEloTarget) {
        Optional<Question> primary = selectNextQuestion(user, answeredIds, topicFilter, topicList, companyEloTarget);
        if (primary.isPresent()) return primary;

        String topic = (topicFilter == null || topicFilter.equals("adaptive")) ? null : topicFilter;
        List<Long> excluded = answeredIds.isEmpty() ? List.of(-1L) : answeredIds;
        double targetElo = user.getEloRating() + CHALLENGE_OFFSET;

        for (double window : new double[]{400, 800, 1600, 10000}) {
            List<Question> candidates = questionRepository.findCandidates(
                    excluded, targetElo - window, targetElo + window, topic);
            if (candidates.isEmpty()) {
                candidates = questionRepository.findBroadCandidates(excluded, topic, targetElo, PageRequest.of(0, 20));
            }
            if (!candidates.isEmpty()) {
                return Optional.ofNullable(pickBest(candidates, targetElo, user));
            }
        }

        if (topic != null) {
            List<Question> anyTopic = questionRepository.findBroadCandidates(excluded, null, targetElo, PageRequest.of(0, 20));
            if (!anyTopic.isEmpty()) {
                return Optional.ofNullable(pickBest(anyTopic, targetElo, user));
            }
        }

        List<Question> any = questionRepository.findBroadCandidates(List.of(-1L), null, targetElo, PageRequest.of(0, 20));
        return any.isEmpty() ? Optional.empty() : Optional.ofNullable(pickBest(any, targetElo, user));
    }

    private Question pickBest(List<Question> candidates, double targetElo, User user) {
        Map<String, Double> topicPerf = topicPerformanceRepository.findByUserId(user.getId()).stream()
                .collect(Collectors.toMap(TopicPerformance::getTopic, TopicPerformance::getProficiency));

        Question best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Question q : candidates) {
            double eloDiff = Math.abs(q.getEloRating() - targetElo);
            double eloScore = 1 - Math.min(1, eloDiff / SELECTION_WINDOW);

            double topicScore = 1.0;
            Double prof = topicPerf.get(q.getTopic());
            if (prof != null && prof < 40) {
                topicScore = TOPIC_WEIGHT_FACTOR;
            }

            double randomness = ThreadLocalRandom.current().nextDouble(0.9, 1.1);
            double finalScore = eloScore * topicScore * randomness;
            if (finalScore > bestScore) {
                bestScore = finalScore;
                best = q;
            }
        }
        return best;
    }

    public double calculateQuestionScore(int testsPassed, int testsTotal, int timeTaken, int hintsUsed, int timeLimit) {
        if (testsTotal == 0) return 0.0;

        double baseScore = (testsPassed / (double) testsTotal) * 10;
        double hintPenalty = Math.min(2.0, hintsUsed * 0.5);

        double timeRatio = timeTaken / (double) timeLimit;
        double timeBonus;
        if (timeRatio <= 0.33) timeBonus = 0.5;
        else if (timeRatio <= 0.66) timeBonus = 0.0;
        else timeBonus = -0.5;

        double finalScore = Math.min(10.0, Math.max(0, baseScore - hintPenalty + timeBonus));
        return Math.round(finalScore * 100) / 100.0;
    }

    public double[] updateRatingsAfterQuestion(User user, Question question, double score, double maxScore) {
        double actual = Math.min(1.0, score / maxScore);
        double expected = EloEngine.expectedScore(user.getEloRating(), question.getEloRating());

        double newUserElo = EloEngine.updateUserElo(user.getEloRating(), expected, actual);
        double newQElo = EloEngine.updateQuestionElo(question.getEloRating(), expected, actual);

        return new double[]{newUserElo, newQElo};
    }

    @Transactional
    public void updateTopicPerformance(User user, String topic, boolean solved, double score) {
        TopicPerformance tp = topicPerformanceRepository.findByUserIdAndTopic(user.getId(), topic)
                .orElseGet(() -> {
                    TopicPerformance created = new TopicPerformance();
                    created.setUser(user);
                    created.setTopic(topic);
                    return created;
                });

        int attempted = tp.getQuestionsAttempted() + 1;
        int solvedCount = tp.getQuestionsSolved() + (solved ? 1 : 0);

        double alpha = 0.3;
        double newScore = Math.min(100.0, (score / 10.0) * 100);
        double proficiency = Math.min(100.0, alpha * newScore + (1 - alpha) * tp.getProficiency());
        double avgScore = ((tp.getAvgScore() * (attempted - 1)) + score) / attempted;

        tp.setQuestionsAttempted(attempted);
        tp.setQuestionsSolved(solvedCount);
        tp.setProficiency(proficiency);
        tp.setAvgScore(avgScore);
        tp.setLastPracticed(Instant.now());

        topicPerformanceRepository.save(tp);
    }

    @Transactional
    public void updateTopicPerformanceOnResubmit(User user, String topic, boolean solved, double score,
                                                  boolean wasSolved) {
        TopicPerformance tp = topicPerformanceRepository.findByUserIdAndTopic(user.getId(), topic)
                .orElseGet(() -> {
                    TopicPerformance created = new TopicPerformance();
                    created.setUser(user);
                    created.setTopic(topic);
                    return created;
                });

        int solvedCount = tp.getQuestionsSolved();
        if (wasSolved && !solved) {
            solvedCount = Math.max(0, solvedCount - 1);
        } else if (!wasSolved && solved) {
            solvedCount = solvedCount + 1;
        }

        double alpha = 0.3;
        double newScore = Math.min(100.0, (score / 10.0) * 100);
        double proficiency = Math.min(100.0, alpha * newScore + (1 - alpha) * tp.getProficiency());
        int attempted = tp.getQuestionsAttempted();
        double avgScore = attempted > 0
                ? ((tp.getAvgScore() * attempted) - tp.getAvgScore() + score) / attempted
                : score;

        tp.setQuestionsSolved(solvedCount);
        tp.setProficiency(proficiency);
        tp.setAvgScore(avgScore);
        tp.setLastPracticed(Instant.now());
        topicPerformanceRepository.save(tp);
    }
}
