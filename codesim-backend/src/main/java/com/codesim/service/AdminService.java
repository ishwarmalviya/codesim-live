package com.codesim.service;

import com.codesim.dto.AdminDtos.*;
import com.codesim.entity.Question;
import com.codesim.entity.User;
import com.codesim.repository.InterviewRepository;
import com.codesim.repository.QuestionRepository;
import com.codesim.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.format.TextStyle;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class AdminService {

    private static final Map<String, Double> ELO_MAP = Map.of(
            "easy", 900.0, "medium", 1200.0, "hard", 1550.0, "expert", 1800.0
    );

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final InterviewRepository interviewRepository;
    private final AuthService authService;

    public AdminService(UserRepository userRepository, QuestionRepository questionRepository,
                         InterviewRepository interviewRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.interviewRepository = interviewRepository;
        this.authService = authService;
    }

    public AdminDashboardResponse getDashboard() {
        AdminDashboardResponse resp = new AdminDashboardResponse();
        resp.totalUsers = userRepository.countByRole("USER");
        resp.activeToday = userRepository.countByRoleAndLastActiveGreaterThanEqual("USER", Instant.now().minus(1, ChronoUnit.DAYS));
        resp.totalQuestions = questionRepository.count();
        resp.totalInterviews = interviewRepository.countByStatus("completed");
        resp.interviewsToday = interviewRepository.countByStatusAndCompletedAtGreaterThanEqual("completed", Instant.now().minus(1, ChronoUnit.DAYS));
        Double avg = interviewRepository.avgScorePercent("completed");
        resp.avgScore = avg == null ? 0.0 : Math.round(avg * 10) / 10.0;

        resp.recentUsers = userRepository.findByRoleOrderByCreatedAtDesc("USER").stream()
                .limit(5).map(authService::toDto).toList();

        Map<String, Long> topicStats = new LinkedHashMap<>();
        for (Object[] row : questionRepository.countByTopic()) {
            topicStats.put((String) row[0], (Long) row[1]);
        }
        resp.topicStats = topicStats;

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            Instant dayStart = Instant.now().minus(i, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
            Instant dayEnd = dayStart.plus(1, ChronoUnit.DAYS);
            long count = interviewRepository.countByStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan("completed", dayStart, dayEnd);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", dayStart.atZone(ZoneOffset.UTC).getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            point.put("count", count);
            trend.add(point);
        }
        resp.trendData = trend;

        Map<String, Long> diffStats = new LinkedHashMap<>();
        for (Object[] row : questionRepository.countByDifficulty()) {
            diffStats.put((String) row[0], (Long) row[1]);
        }
        resp.diffStats = diffStats;

        return resp;
    }

    public Page<User> searchUsers(String search, int page) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), 20);
        if (search == null || search.isBlank()) {
            return userRepository.findByRole("USER", pageable);
        }
        return userRepository.findByRoleAndNameContainingIgnoreCaseOrRoleAndEmailContainingIgnoreCase(
                "USER", search, "USER", search, pageable);
    }

    @Transactional
    public User toggleUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setIsActiveAccount(!user.getIsActiveAccount());
        return userRepository.save(user);
    }

    public Page<Question> searchQuestions(String search, String difficulty, String topic, int page) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), 20);
        return questionRepository.search(
                search == null || search.isBlank() ? null : "%" + search.toLowerCase() + "%",
                difficulty == null || difficulty.isBlank() ? null : difficulty,
                topic == null || topic.isBlank() ? null : topic,
                pageable);
    }

    public Question getQuestion(Long id) {
        return questionRepository.findById(id).orElseThrow();
    }

    @Transactional
    public Question saveQuestion(Long questionId, QuestionUpsertRequest req, Long adminId) {
        if (req.title == null || req.title.isBlank()) {
            throw new IllegalArgumentException("Title is required.");
        }
        Question question = questionId != null
                ? questionRepository.findById(questionId).orElseThrow()
                : new Question();

        question.setTitle(req.title.trim());
        question.setDescription(req.description == null ? "" : req.description.trim());
        String difficulty = req.difficulty == null ? "medium" : req.difficulty;
        question.setDifficulty(difficulty);
        question.setEloRating(ELO_MAP.getOrDefault(difficulty, 1200.0));
        question.setTopic(req.topic == null ? "" : req.topic.trim());
        question.setSubtopic(req.subtopic == null ? "" : req.subtopic.trim());
        question.setStarterCode(req.starterCode == null ? "" : req.starterCode.trim());
        question.setSolutionCode(req.solutionCode == null ? "" : req.solutionCode.trim());
        question.setFunctionSignature(req.functionSignature == null ? "" : req.functionSignature.trim());
        question.setTestCases(req.testCases == null || req.testCases.isBlank() ? "[]" : req.testCases);
        question.setHints(req.hints == null || req.hints.isBlank() ? "[]" : req.hints);
        question.setConstraints(req.constraints == null ? "" : req.constraints.trim());
        question.setExamples(req.examples == null ? "" : req.examples.trim());
        question.setTimeComplexity(req.timeComplexity == null ? "" : req.timeComplexity.trim());
        question.setSpaceComplexity(req.spaceComplexity == null ? "" : req.spaceComplexity.trim());
        question.setTags(req.tags == null ? "" : req.tags.trim());
        question.setIsActive(true);
        question.setCreatedBy(adminId);

        return questionRepository.save(question);
    }

    @Transactional
    public void deactivateQuestion(Long questionId) {
        Question q = questionRepository.findById(questionId).orElseThrow();
        q.setIsActive(false);
        questionRepository.save(q);
    }

    public AdminAnalyticsResponse getAnalytics() {
        AdminAnalyticsResponse resp = new AdminAnalyticsResponse();

        resp.topUsers = userRepository.findByRoleAndIsActiveAccountTrueOrderByEloRatingDesc("USER", PageRequest.of(0, 10))
                .stream().map(authService::toDto).toList();

        List<Map<String, Object>> topicStats = new ArrayList<>();
        for (Object[] row : questionRepository.topicAnalytics()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("topic", row[0]);
            m.put("total", row[1]);
            m.put("avgElo", row[2] == null ? 0 : Math.round(((Number) row[2]).doubleValue() * 10) / 10.0);
            m.put("attempts", row[3]);
            m.put("solved", row[4]);
            topicStats.add(m);
        }
        resp.topicStats = topicStats;

        Map<String, Long> scoreDist = new LinkedHashMap<>();
        scoreDist.put("0-25", 0L);
        scoreDist.put("26-50", 0L);
        scoreDist.put("51-75", 0L);
        scoreDist.put("76-100", 0L);
        for (var iv : interviewRepository.findByStatusAndMaxScoreGreaterThan("completed", 0.0)) {
            double pct = iv.getScorePercent();
            if (pct <= 25) scoreDist.merge("0-25", 1L, Long::sum);
            else if (pct <= 50) scoreDist.merge("26-50", 1L, Long::sum);
            else if (pct <= 75) scoreDist.merge("51-75", 1L, Long::sum);
            else scoreDist.merge("76-100", 1L, Long::sum);
        }
        resp.scoreDist = scoreDist;

        List<Map<String, Object>> growth = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            Instant dayStart = Instant.now().minus(i, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
            Instant dayEnd = dayStart.plus(1, ChronoUnit.DAYS);
            long count = userRepository.countByRoleAndCreatedAtGreaterThanEqualAndCreatedAtLessThan("USER", dayStart, dayEnd);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", dayStart.atZone(ZoneOffset.UTC).getMonthValue() + "/" + dayStart.atZone(ZoneOffset.UTC).getDayOfMonth());
            point.put("count", count);
            growth.add(point);
        }
        resp.userGrowth = growth;

        return resp;
    }
}
