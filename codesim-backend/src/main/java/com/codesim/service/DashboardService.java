package com.codesim.service;

import com.codesim.dto.DashboardDtos.*;
import com.codesim.entity.Interview;
import com.codesim.entity.TopicPerformance;
import com.codesim.entity.User;
import com.codesim.repository.InterviewRepository;
import com.codesim.repository.TopicPerformanceRepository;
import com.codesim.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DashboardService {

    private static final List<String> ALL_TOPICS = List.of(
            "Arrays", "Strings", "Trees", "Graphs", "Dynamic Programming",
            "Sorting", "Binary Search", "Linked Lists", "Recursion", "Hashing"
    );

    private final InterviewRepository interviewRepository;
    private final TopicPerformanceRepository topicPerformanceRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final AiEngineService aiEngineService;
    private final ObjectMapper objectMapper;
    private final InterviewService interviewService;

    public DashboardService(InterviewRepository interviewRepository,
                             TopicPerformanceRepository topicPerformanceRepository,
                             UserRepository userRepository,
                             AuthService authService,
                             AiEngineService aiEngineService,
                             ObjectMapper objectMapper,
                             InterviewService interviewService) {
        this.interviewRepository = interviewRepository;
        this.topicPerformanceRepository = topicPerformanceRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.aiEngineService = aiEngineService;
        this.objectMapper = objectMapper;
        this.interviewService = interviewService;
    }

    private List<TopicPerfDto> toTopicPerfDtos(List<TopicPerformance> perfs) {
        List<TopicPerfDto> out = new ArrayList<>();
        for (TopicPerformance tp : perfs) {
            TopicPerfDto dto = new TopicPerfDto();
            dto.topic = tp.getTopic();
            dto.proficiency = Math.round(tp.getProficiency() * 10) / 10.0;
            dto.avgScore = Math.round(tp.getAvgScore() * 10) / 10.0;
            dto.questionsAttempted = tp.getQuestionsAttempted();
            dto.questionsSolved = tp.getQuestionsSolved();
            dto.successRate = tp.getSuccessRate();
            out.add(dto);
        }
        return out;
    }

    public DashboardResponse getDashboard(User user) {
        DashboardResponse resp = new DashboardResponse();
        resp.user = authService.toDto(user);

        List<Interview> recent = interviewRepository.findByUserIdAndStatusOrderByCompletedAtDesc(user.getId(), "completed");
        List<Interview> recentTop5 = recent.stream().limit(5).toList();
        resp.recentInterviews = recentTop5.stream().map(interviewService::toInterviewDto).toList();

        List<TopicPerformance> perfs = topicPerformanceRepository.findByUserIdOrderByProficiencyAsc(user.getId());
        resp.topicPerfs = toTopicPerfDtos(perfs);

        List<Interview> chartSource = new ArrayList<>(recent.stream().limit(10).toList());
        Collections.reverse(chartSource);
        ChartData chart = new ChartData();
        chart.labels = chartSource.stream().map(i -> "#" + i.getId()).toList();
        chart.scores = chartSource.stream().map(Interview::getScorePercent).toList();
        chart.elos = chartSource.stream().map(i -> (double) Math.round(i.getEloAfter())).toList();
        resp.chartData = chart;

        var topicMap = new java.util.HashMap<String, Double>();
        for (TopicPerformance tp : perfs) topicMap.put(tp.getTopic(), tp.getProficiency());
        RadarData radar = new RadarData();
        radar.topics = ALL_TOPICS;
        radar.values = ALL_TOPICS.stream().map(t -> Math.round(topicMap.getOrDefault(t, 0.0) * 10) / 10.0).toList();
        resp.radarData = radar;

        resp.streak = calculateStreak(recent);
        resp.rank = getUserRank(user);

        return resp;
    }

    public ProfileResponse getProfile(User user) {
        ProfileResponse resp = new ProfileResponse();
        resp.user = authService.toDto(user);

        List<TopicPerformance> perfs = topicPerformanceRepository.findByUserId(user.getId());
        resp.topicPerfs = toTopicPerfDtos(perfs);

        List<Interview> allInterviews = interviewRepository.findByUserIdAndStatusOrderByCompletedAtDesc(user.getId(), "completed");
        resp.allInterviews = allInterviews.stream().map(interviewService::toInterviewDto).toList();

        try {
            var recentData = objectMapper.createArrayNode();
            for (Interview i : allInterviews.stream().limit(5).toList()) {
                var node = objectMapper.createObjectNode();
                node.put("date", i.getCompletedAt() != null ? i.getCompletedAt().toString().substring(0, 10) : "");
                node.put("score_percent", i.getScorePercent());
                node.put("topic", i.getSelectedTopic());
                node.put("questions", i.getQuestionsAnswered());
                recentData.add(node);
            }
            resp.aiInsights = aiEngineService.getInterviewSummary(user.getName(), recentData.toString());
        } catch (Exception e) {
            resp.aiInsights = null;
        }

        resp.rank = getUserRank(user);
        return resp;
    }

    public User updateProfile(User user, com.codesim.dto.DashboardDtos.UpdateProfileRequest req,
                               org.springframework.security.crypto.password.PasswordEncoder encoder) {
        if (req.name != null && !req.name.isBlank()) user.setName(req.name.trim());
        if (req.bio != null) user.setBio(req.bio.length() > 500 ? req.bio.substring(0, 500) : req.bio);
        if (req.college != null) user.setCollege(req.college.trim());
        if (req.preferredLanguage != null) user.setPreferredLanguage(req.preferredLanguage);
        if (req.themePreference != null && (req.themePreference.equals("light") || req.themePreference.equals("dark"))) {
            user.setThemePreference(req.themePreference);
        }
        if (req.newPassword != null && !req.newPassword.isBlank()) {
            if (req.newPassword.length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters.");
            }
            user.setPasswordHash(encoder.encode(req.newPassword));
        }
        return userRepository.save(user);
    }

    public List<LeaderboardEntry> getLeaderboard() {
        var users = userRepository.findByRoleAndIsActiveAccountTrueOrderByEloRatingDesc("USER",
                org.springframework.data.domain.PageRequest.of(0, 20));
        List<LeaderboardEntry> out = new ArrayList<>();
        int rank = 1;
        for (User u : users) {
            LeaderboardEntry e = new LeaderboardEntry();
            e.rank = rank++;
            e.name = u.getName();
            e.elo = Math.round(u.getEloRating());
            e.level = u.getSkillLevel();
            e.interviews = u.getTotalInterviews();
            e.college = u.getCollege() == null || u.getCollege().isBlank() ? "N/A" : u.getCollege();
            out.add(e);
        }
        return out;
    }

    private int getUserRank(User user) {
        return (int) userRepository.countByRoleAndEloRatingGreaterThan("USER", user.getEloRating()) + 1;
    }

    private int calculateStreak(List<Interview> completedInterviews) {
        if (completedInterviews.isEmpty()) return 0;

        int streak = 0;
        LocalDate checkDate = LocalDate.now(ZoneOffset.UTC);

        for (Interview iv : completedInterviews) {
            if (iv.getCompletedAt() == null) continue;
            LocalDate ivDate = iv.getCompletedAt().atZone(ZoneOffset.UTC).toLocalDate();
            if (ivDate.equals(checkDate)) {
                streak++;
                checkDate = checkDate.minusDays(1);
            } else if (ivDate.equals(checkDate.minusDays(1))) {
                checkDate = ivDate;
                streak++;
                checkDate = checkDate.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }
}
