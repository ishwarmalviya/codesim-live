package com.codesim.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public class QuestionDto {
    public Long id;
    public String title;
    public String description;
    public String difficulty;
    public String topic;
    public String subtopic;
    public Double eloRating;
    public String starterCode;
    public String functionSignature;
    public JsonNode hints;
    public JsonNode testCases; // visible test cases only (hidden ones stripped)
    public String constraints;
    public String examples;
    public String timeComplexity;
    public String spaceComplexity;
    public List<String> tags;
    public Double successRate;
    public Integer timesAttempted;
    public List<String> supportedLanguages;
    public JsonNode starterCodeByLang;
}
