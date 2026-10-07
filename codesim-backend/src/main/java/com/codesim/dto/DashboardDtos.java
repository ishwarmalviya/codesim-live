package com.codesim.dto;

import com.codesim.dto.AuthDtos.UserDto;
import com.codesim.dto.InterviewDtos.InterviewDto;

import java.util.List;

public class DashboardDtos {

    public static class TopicPerfDto {
        public String topic;
        public double proficiency;
        public double avgScore;
        public int questionsAttempted;
        public int questionsSolved;
        public double successRate;
    }

    public static class ChartData {
        public List<String> labels;
        public List<Double> scores;
        public List<Double> elos;
    }

    public static class RadarData {
        public List<String> topics;
        public List<Double> values;
    }

    public static class DashboardResponse {
        public UserDto user;
        public List<InterviewDto> recentInterviews;
        public List<TopicPerfDto> topicPerfs;
        public ChartData chartData;
        public RadarData radarData;
        public int streak;
        public int rank;
    }

    public static class ProfileResponse {
        public UserDto user;
        public List<TopicPerfDto> topicPerfs;
        public List<InterviewDto> allInterviews;
        public Object aiInsights;
        public int rank;
    }

    public static class UpdateProfileRequest {
        public String name;
        public String bio;
        public String college;
        public String preferredLanguage;
        public String themePreference;
        public String newPassword;
    }

    public static class LeaderboardEntry {
        public int rank;
        public String name;
        public double elo;
        public String level;
        public int interviews;
        public String college;
    }
}
