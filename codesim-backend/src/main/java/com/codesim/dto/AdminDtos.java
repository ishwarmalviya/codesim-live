package com.codesim.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class AdminDtos {

    public static class AdminUserDto {
        public Long id;
        public String name;
        public String email;
        public String college;
        public Double eloRating;
        public String skillLevel;
        public Integer totalInterviews;
        public Double avgScore;
        public Instant createdAt;
        public Boolean isActiveAccount;
    }

    public static class AdminDashboardResponse {
        public long totalUsers;
        public long activeToday;
        public long totalQuestions;
        public long totalInterviews;
        public long interviewsToday;
        public double avgScore;
        public List<AuthDtos.UserDto> recentUsers;
        public Map<String, Long> topicStats;
        public List<Map<String, Object>> trendData;
        public Map<String, Long> diffStats;
    }

    public static class AdminAnalyticsResponse {
        public List<AuthDtos.UserDto> topUsers;
        public List<Map<String, Object>> topicStats;
        public Map<String, Long> scoreDist;
        public List<Map<String, Object>> userGrowth;
    }

    public static class AdminQuestionDetailDto {
        public Long id;
        public String title;
        public String description;
        public String difficulty;
        public String topic;
        public String subtopic;
        public String starterCode;
        public String solutionCode;
        public String functionSignature;
        public String testCases; // raw JSON string, including hidden cases
        public String hints;     // raw JSON string
        public String constraints;
        public String examples;
        public String timeComplexity;
        public String spaceComplexity;
        public String tags;
    }

    public static class QuestionUpsertRequest {
        public String title;
        public String description;
        public String difficulty;
        public String topic;
        public String subtopic;
        public String starterCode;
        public String solutionCode;
        public String functionSignature;
        public String testCases; // raw JSON string
        public String hints;     // raw JSON string
        public String constraints;
        public String examples;
        public String timeComplexity;
        public String spaceComplexity;
        public String tags;
    }
}
