package com.codesim.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public class PhaseThreeDtos {

    public static class ReplayEventDto {
        public Long id;
        public String eventType;
        public String codeSnapshot;
        public JsonNode payload;
        public Instant createdAt;
    }

    public static class PlacementScoreDto {
        public Double overallScore;
        public JsonNode breakdown;
        public Instant computedAt;
    }

    public static class CandidateDto {
        public Long userId;
        public String name;
        public String email;
        public String college;
        public Double eloRating;
        public String skillLevel;
        public Double placementScore;
        public JsonNode breakdown;
        public boolean shortlisted;
        public String note;
    }

    public static class ShortlistRequest {
        public String note;
    }
}
