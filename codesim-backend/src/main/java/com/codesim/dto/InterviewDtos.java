package com.codesim.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;

public class InterviewDtos {

    public static class StartInterviewRequest {
        public String type = "adaptive";
        public String topic = "adaptive";
        public int numQuestions = 5;
        public List<String> topics;
        public Long targetCompanyId;
        public String targetRole = "";
        public Long questionId;
    }

    public static class InterviewDto {
        public Long id;
        public String status;
        public String interviewType;
        public String selectedTopic;
        public Integer totalQuestions;
        public Integer questionsAnswered;
        public Double score;
        public Double maxScore;
        public Double scorePercent;
        public Double eloBefore;
        public Double eloAfter;
        public Instant startedAt;
        public Instant completedAt;
        public Double durationMinutes;
    }

    public static class NextQuestionResponse {
        public boolean completed;
        public Long interviewQuestionId;
        public Integer questionNumber;
        public Integer totalQuestions;
        public QuestionDto question;
        public Integer timeLimitSeconds = 1800;
    }

    public static class SubmitRequest {
        public Long iqId;
        public String code;
        public int timeTaken;
    }

    public static class SubmitResponse {
        public boolean success;
        public List<TestResultDto> testResults;
        public int passed;
        public int total;
        public boolean allPassed;
        public double score;
        public double maxScore = 10.0;
        public double eloChange;
        public JsonNode review;
        public JsonNode analysis;
    }

    public static class TestResultDto {
        public int testNum;
        public boolean passed;
        public String description;
        public String error;
        public Object expected;
        public Object actual;
        public boolean hidden;
    }

    public static class RunRequest {
        public String code;
        public String input = "";
        public String language = "python";
        public List<Object> args;
        public String callExpr;
    }

    public static class HintRequest {
        public Long iqId;
        public String code;
    }

    public static class ResultsResponse {
        public InterviewDto interview;
        public Double eloChange;
        public JsonNode aiSummary;
        public List<QuestionResultDto> questions;
    }

    public static class QuestionResultDto {
        public Long iqId;
        public QuestionDto question;
        public String userCode;
        public double score;
        public double maxScore;
        public int testsPassed;
        public int testsTotal;
        public int hintsUsed;
        public String status;
        public JsonNode review;
    }
}
