package com.codesim.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;

public class PhaseTwoDtos {

    public static class CompanyDto {
        public Long id;
        public String name;
        public String logoEmoji;
        public Double difficultyThreshold;
        public String tags;
        public JsonNode requiredTopics;
        public boolean targeted;
        public Double readinessScore; // null if not yet targeted/computed
    }

    public static class SkillGapDto {
        public String topic;
        public Double requiredProficiency;
        public Double actualProficiency;
        public Double gapSeverity;
    }

    public static class RoadmapItemDto {
        public Long id;
        public String topic;
        public Long questionId;
        public String questionTitle;
        public Integer priorityRank;
        public String status;
        public String reason;
    }

    public static class InterviewDnaDto {
        public JsonNode traits;
        public String narrativeText;
        public Instant computedAt;
    }

    public static class ResumeDto {
        public String rawText;
        public List<String> parsedSkills;
        public String experienceLevel;
        public Instant uploadedAt;
        public Integer atsScore;
        public JsonNode analysis;
    }

    public static class RoadmapGenerateRequest {
        public String skillLevel;
        public String placementGoal;
        public List<Long> targetCompanyIds;
        public boolean useSavedResume = true;
        public String resumeText;
    }

    public static class ResumeUploadRequest {
        public String rawText;
    }
}
