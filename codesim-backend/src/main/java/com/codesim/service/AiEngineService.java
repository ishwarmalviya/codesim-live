package com.codesim.service;

import com.codesim.entity.Interview;
import com.codesim.entity.InterviewQuestion;
import com.codesim.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiEngineService {

    private static final Logger log = LoggerFactory.getLogger(AiEngineService.class);
    private static final Pattern JSON_BLOCK = Pattern.compile("\\{.*}", Pattern.DOTALL);

    @Value("${app.openai.api-key}")
    private String apiKey;

    @Value("${app.openai.model}")
    private String model;

    private RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private boolean hasKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    public boolean hasApiKey() {
        return hasKey();
    }

    @PostConstruct
    void logApiKeyStatus() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(12));
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .requestFactory(factory)
                .build();
        if (hasKey()) {
            log.info("OpenAI API key configured — AI features enabled");
        } else {
            log.warn("OPENAI_API_KEY not set — using rule-based fallbacks for AI features");
        }
    }

    private String callChat(String prompt, int maxTokens) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_completion_tokens", maxTokens);
        var messages = objectMapper.createArrayNode();
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);
        body.set("messages", messages);

        JsonNode response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        return response.path("choices").get(0).path("message").path("content").asText("");
    }

    private JsonNode extractJson(String content) {
        Matcher m = JSON_BLOCK.matcher(content);
        if (m.find()) {
            try {
                return objectMapper.readTree(m.group());
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public JsonNode getCodeReview(String questionTitle, String questionDescription, String userCode,
                                  int testsPassed, int testsTotal) {
        return getCodeReview(questionTitle, questionDescription, userCode, testsPassed, testsTotal, "python");
    }

    public JsonNode getCodeReview(String questionTitle, String questionDescription, String userCode,
                                  int testsPassed, int testsTotal, String language) {
        if (!hasKey()) return mockReview(questionTitle, testsPassed, testsTotal);

        try {
            String lang = language != null ? language : "python";
            String prompt = """
                You are an expert software engineering interviewer providing constructive code review.

                **Problem:** %s
                **Description:** %s

                **Candidate's Code:**
                ```%s
                %s
                ```

                **Test Results:** %d / %d tests passed.

                Provide a detailed, structured code review. Be encouraging but honest. Return JSON with exactly these fields:
                {
                  "overall_assessment": "2-3 sentence overall assessment",
                  "score_out_of_10": <integer 0-10>,
                  "strengths": ["strength 1", "strength 2", "strength 3"],
                  "improvements": ["improvement 1", "improvement 2", "improvement 3"],
                  "time_complexity": "Big-O analysis",
                  "space_complexity": "Big-O analysis",
                  "code_quality": {"readability": <1-10>, "efficiency": <1-10>, "correctness": <1-10>, "style": <1-10>},
                  "optimized_approach": "Brief description of optimal approach (no full code)",
                  "key_concepts": ["concept1", "concept2"],
                  "interview_tip": "One specific tip for interview performance"
                }
                """.formatted(questionTitle, questionDescription, lang, userCode, testsPassed, testsTotal);

            String content = callChat(prompt, 700);
            JsonNode json = extractJson(content);
            return json != null ? json : mockReview(questionTitle, testsPassed, testsTotal);
        } catch (Exception e) {
            return mockReview(questionTitle, testsPassed, testsTotal);
        }
    }

    public String getHint(String questionTitle, String questionDescription, String userCode, int hintNumber) {
        if (!hasKey()) return mockHint(hintNumber);

        try {
            List<String> specificity = List.of(
                    "very general (no algorithm name, just think about the problem differently)",
                    "moderate (hint at the data structure or approach)",
                    "specific (mention the algorithm or key insight clearly)"
            );
            String level = specificity.get(Math.min(hintNumber - 1, 2));

            String prompt = """
                You are a coding interview coach. Give a %s hint for:

                **Problem:** %s
                **Description:** %s

                **Candidate's current code:**
                ```python
                %s
                ```

                Provide ONE concise hint (2-3 sentences max). Do NOT give the solution. Be Socratic — guide their thinking.
                """.formatted(level, questionTitle, questionDescription,
                    userCode == null || userCode.isBlank() ? "(no code written yet)" : userCode);

            return callChat(prompt, 200).trim();
        } catch (Exception e) {
            return mockHint(hintNumber);
        }
    }

    public JsonNode getInterviewSummary(String userName, String recentDataJson) {
        if (!hasKey()) {
            ObjectNode fallback = objectMapper.createObjectNode();
            fallback.put("summary", "Complete more interviews to get AI-powered insights.");
            fallback.putArray("focus_areas");
            return fallback;
        }
        try {
            String prompt = """
                Analyze %s's coding interview performance and provide insights.

                Recent performance data:
                %s

                Return JSON:
                {
                  "summary": "2-3 sentence performance summary",
                  "strengths": ["strength 1", "strength 2"],
                  "focus_areas": ["area to improve 1", "area to improve 2"],
                  "next_steps": ["specific action 1", "specific action 2"],
                  "predicted_level": "Beginner/Intermediate/Advanced/Expert"
                }
                """.formatted(userName, recentDataJson);

            String content = callChat(prompt, 600);
            JsonNode json = extractJson(content);
            if (json != null) return json;
        } catch (Exception ignored) {
        }
        ObjectNode fallback = objectMapper.createObjectNode();
        fallback.put("summary", "Keep practicing to unlock AI insights!");
        fallback.putArray("focus_areas");
        return fallback;
    }

    public String getInterviewDnaNarrative(String userName, String traitsJson) {
        if (!hasKey()) {
            return buildFallbackNarrative(userName, traitsJson);
        }
        try {
            String prompt = """
                Write a short (3-4 sentence), encouraging "Interview DNA" personality profile for a coding
                interview candidate named %s, based on these behavioral traits derived from their interview history:

                %s

                Describe them like a coach summarizing their interview style (e.g. speed vs. thoroughness,
                reliance on hints, growth trend, favorite topics). Be specific and personal, not generic.
                Return plain text only, no JSON, no markdown headers.
                """.formatted(userName, traitsJson);

            return callChat(prompt, 300).trim();
        } catch (Exception e) {
            return buildFallbackNarrative(userName, traitsJson);
        }
    }

    private String buildFallbackNarrative(String userName, String traitsJson) {
        try {
            JsonNode t = objectMapper.readTree(traitsJson);
            if (t.path("status").asText("").equals("insufficient_data") || !t.has("speed_profile")) {
                return "Complete a few more interviews and " + userName + "'s Interview DNA profile will start to take shape.";
            }

            String speed = t.path("speed_profile").asText("balanced");
            String persistence = t.path("persistence_profile").asText("independent");
            String trend = t.path("code_quality_trend").asText("stable");
            double solveRate = t.path("solve_rate_percent").asDouble(0);
            List<String> favTopics = new java.util.ArrayList<>();
            t.path("favorite_topics").forEach(n -> favTopics.add(n.asText()));

            String persistencePhrase = switch (persistence) {
                case "hint-reliant" -> "leans on hints frequently while working through problems";
                case "occasional-hint-user" -> "works mostly independently, reaching for a hint now and then";
                default -> "works through problems independently, rarely needing a hint";
            };

            StringBuilder sb = new StringBuilder();
            sb.append(userName).append(" tends to be a ").append(speed).append(" solver who ")
                    .append(persistencePhrase).append(", ");
            sb.append("solving about ").append(Math.round(solveRate)).append("% of attempted questions outright. ");
            sb.append("Code quality has been ").append(trend).append(" across recent interviews");
            if (!favTopics.isEmpty()) {
                sb.append(", with the most practice time going into ").append(joinWithAnd(favTopics));
            }
            sb.append(".");
            return sb.toString();
        } catch (Exception e) {
            return "Keep completing interviews to unlock a richer Interview DNA profile.";
        }
    }

    private String joinWithAnd(List<String> items) {
        if (items.size() == 1) return items.get(0);
        if (items.size() == 2) return items.get(0) + " and " + items.get(1);
        return String.join(", ", items.subList(0, items.size() - 1)) + ", and " + items.get(items.size() - 1);
    }

    private JsonNode mockReview(String title, int passed, int total) {
        int score = total > 0 ? Math.round((passed / (float) total) * 10) : 5;
        ObjectNode n = objectMapper.createObjectNode();
        n.put("overall_assessment", "You solved " + passed + "/" + total + " test cases. Good attempt at '" + title + "'.");
        n.put("score_out_of_10", score);
        var strengths = n.putArray("strengths");
        strengths.add("Attempted the problem").add("Code is structured").add("Shows problem-solving effort");
        var improvements = n.putArray("improvements");
        improvements.add("Review edge cases").add("Consider time complexity").add("Add comments for clarity");
        n.put("time_complexity", "Analysis not available — add OPENAI_API_KEY for AI review");
        n.put("space_complexity", "O(n) estimated");
        ObjectNode quality = n.putObject("code_quality");
        quality.put("readability", 7).put("efficiency", 6).put("correctness", score).put("pythonic_style", 7);
        n.put("optimized_approach", "Consider the optimal algorithm for this problem type.");
        var concepts = n.putArray("key_concepts");
        concepts.add("Problem solving").add("Algorithm design");
        n.put("interview_tip", "Always discuss your approach before coding in interviews.");
        return n;
    }

    private String mockHint(int hintNumber) {
        List<String> hints = List.of(
                "Think about the problem from a different angle. What data structure could help organize the information?",
                "Consider what information you need to track as you iterate through the input.",
                "Look at the constraints — they often hint at the expected time complexity and algorithm type."
        );
        return hints.get(Math.min(hintNumber - 1, 2));
    }

    public JsonNode analyzeResume(String rawText, List<String> targetCompanies) {
        if (!hasKey()) return mockResumeAnalysis(rawText);
        try {
            String companies = targetCompanies == null || targetCompanies.isEmpty()
                    ? "general tech companies" : String.join(", ", targetCompanies);
            String prompt = """
                Analyze this resume for a coding interview preparation platform. Target companies: %s

                Resume:
                %s

                Return JSON:
                {
                  "experienceLevel": "Fresher|Junior|Experienced|Senior",
                  "yearsOfExperience": <number>,
                  "technicalSkills": ["skill1", "skill2"],
                  "missingSkills": ["skill1", "skill2"],
                  "atsScore": <0-100>,
                  "improvements": ["improvement1", "improvement2", "improvement3"],
                  "companySpecificTips": {"CompanyName": "tip"},
                  "summary": "2-3 sentence summary"
                }
                """.formatted(companies, rawText.substring(0, Math.min(rawText.length(), 8000)));

            String content = callChat(prompt, 1200);
            JsonNode json = extractJson(content);
            return json != null ? json : mockResumeAnalysis(rawText);
        } catch (Exception e) {
            return mockResumeAnalysis(rawText);
        }
    }

    public JsonNode generateCompanyRoadmap(String userName, String companyName, String gapsJson,
                                            List<String> questionTitles) {
        if (!hasKey()) return mockCompanyRoadmap(companyName);
        try {
            String prompt = """
                Create a personalized interview prep roadmap for %s targeting %s.

                Skill gaps (JSON): %s
                Available practice questions: %s

                Return JSON:
                {
                  "dsaRoadmap": [{"topic": "...", "priority": 1, "description": "..."}],
                  "coreSubjects": [{"subject": "OS|DBMS|CN|OOP", "topics": ["..."], "priority": 1}],
                  "skillGapAnalysis": "narrative paragraph",
                  "weeklyPlan": ["week 1 focus", "week 2 focus"],
                  "recommendedQuestions": ["question title 1", "question title 2"]
                }
                """.formatted(userName, companyName, gapsJson,
                    questionTitles != null ? String.join(", ", questionTitles) : "general DSA");

            String content = callChat(prompt, 1500);
            JsonNode json = extractJson(content);
            return json != null ? json : mockCompanyRoadmap(companyName);
        } catch (Exception e) {
            return mockCompanyRoadmap(companyName);
        }
    }

    public JsonNode generateMockInterviewFeedback(Interview interview, List<InterviewQuestion> questions, User user) {
        if (!hasKey()) return mockInterviewFeedback(questions);
        try {
            StringBuilder perf = new StringBuilder();
            for (InterviewQuestion iq : questions) {
                perf.append("- ").append(iq.getQuestion().getTitle())
                        .append(": ").append(iq.getStatus())
                        .append(" (").append(iq.getTestsPassed()).append("/").append(iq.getTestsTotal()).append(" tests)\n");
                if (iq.getUserCode() != null && !iq.getUserCode().isBlank()) {
                    perf.append("  Code snippet: ").append(iq.getUserCode().substring(0, Math.min(300, iq.getUserCode().length()))).append("\n");
                }
            }
            String prompt = """
                You are a senior interviewer. Provide detailed feedback after a coding mock interview.

                Candidate: %s
                Interview type: %s
                Target role: %s
                Performance:
                %s

                Return JSON:
                {
                  "overall_assessment": "2-3 sentences",
                  "hire_recommendation": "Strong Yes|Yes|Maybe|No",
                  "strengths": ["strength1", "strength2", "strength3"],
                  "weaknesses": ["weakness1", "weakness2"],
                  "improvements": ["actionable improvement 1", "improvement 2"],
                  "sample_answers": [{"question": "title", "approach": "ideal approach summary"}]
                }
                """.formatted(user.getName(), interview.getInterviewType(),
                    interview.getTargetRole() != null ? interview.getTargetRole() : "Software Engineer",
                    perf);

            String content = callChat(prompt, 1800);
            JsonNode json = extractJson(content);
            return json != null ? json : mockInterviewFeedback(questions);
        } catch (Exception e) {
            return mockInterviewFeedback(questions);
        }
    }

    public JsonNode generatePersonalizedRoadmap(String profileJson) {
        if (!hasKey()) return mockPersonalizedRoadmap();
        try {
            String prompt = """
                Create a 4-week personalized coding interview preparation roadmap based on:

                %s

                Return JSON:
                {
                  "phases": [{"week": 1, "focus": "...", "topics": ["..."], "goals": ["..."]}],
                  "dailyRoutine": "1-2 hour daily schedule",
                  "weeklyRoutine": "weekly milestone schedule",
                  "milestones": ["milestone1", "milestone2"]
                }
                """.formatted(profileJson);

            String content = callChat(prompt, 1500);
            JsonNode json = extractJson(content);
            return json != null ? json : mockPersonalizedRoadmap();
        } catch (Exception e) {
            return mockPersonalizedRoadmap();
        }
    }

    public JsonNode suggestNextQuestion(String contextJson) {
        if (!hasKey()) return mockQuestionSuggestion();
        try {
            String prompt = """
                Recommend the next coding practice question for this student:

                %s

                Return JSON:
                {
                  "reason": "why this question",
                  "topic": "topic name",
                  "difficulty": "easy|medium|hard",
                  "focusArea": "weak area being addressed"
                }
                """.formatted(contextJson);

            String content = callChat(prompt, 400);
            JsonNode json = extractJson(content);
            return json != null ? json : mockQuestionSuggestion();
        } catch (Exception e) {
            return mockQuestionSuggestion();
        }
    }

    public JsonNode getPlacementImprovements(String breakdownJson, String userName) {
        if (!hasKey()) return mockPlacementImprovements();
        try {
            String prompt = """
                Given placement readiness breakdown for %s:

                %s

                Return JSON:
                {
                  "tips": ["specific tip 1", "tip 2", "tip 3"],
                  "activities": [{"activity": "...", "scoreImpact": "how it helps"}],
                  "priority": "top priority action"
                }
                """.formatted(userName, breakdownJson);

            String content = callChat(prompt, 600);
            JsonNode json = extractJson(content);
            return json != null ? json : mockPlacementImprovements();
        } catch (Exception e) {
            return mockPlacementImprovements();
        }
    }

    private JsonNode mockResumeAnalysis(String rawText) {
        ObjectNode n = objectMapper.createObjectNode();
        String lower = rawText.toLowerCase();
        int years = extractYears(lower);
        n.put("experienceLevel", years >= 3 ? "Experienced" : years >= 1 ? "Junior" : "Fresher");
        n.put("yearsOfExperience", years);
        n.put("atsScore", 55);
        var skills = n.putArray("technicalSkills");
        for (String s : List.of("Python", "Java", "JavaScript", "SQL", "Git")) {
            if (lower.contains(s.toLowerCase())) skills.add(s);
        }
        var missing = n.putArray("missingSkills");
        missing.add("System Design").add("Dynamic Programming");
        var improvements = n.putArray("improvements");
        improvements.add("Add quantified project impact").add("Include relevant DSA skills");
        n.put("summary", "Resume analyzed with rule-based heuristics. Set OPENAI_API_KEY for full AI analysis.");
        return n;
    }

    private int extractYears(String lower) {
        var m = Pattern.compile("(\\d+)\\+?\\s*(year|yrs|yoe)").matcher(lower);
        int max = 0;
        while (m.find()) max = Math.max(max, Integer.parseInt(m.group(1)));
        if (lower.contains("senior")) max = Math.max(max, 5);
        if (lower.contains("intern")) max = Math.max(max, 0);
        return max;
    }

    public JsonNode mockCompanyRoadmap(String company) {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("skillGapAnalysis", "Focus on high-weight topics for " + company + ". Set OPENAI_API_KEY for AI roadmap.");
        var dsa = n.putArray("dsaRoadmap");
        ObjectNode item = dsa.addObject();
        item.put("topic", "Arrays").put("priority", 1).put("description", "Foundation topic");
        return n;
    }

    private JsonNode mockInterviewFeedback(List<InterviewQuestion> questions) {
        ObjectNode n = objectMapper.createObjectNode();
        long solved = questions.stream().filter(q -> "solved".equals(q.getStatus())).count();
        n.put("overall_assessment", "You solved " + solved + "/" + questions.size() + " questions. Keep practicing!");
        n.put("hire_recommendation", solved >= questions.size() / 2 ? "Maybe" : "No");
        var strengths = n.putArray("strengths");
        strengths.add("Completed the interview").add("Showed persistence");
        var weaknesses = n.putArray("weaknesses");
        weaknesses.add("Review weak topics").add("Practice more medium problems");
        var improvements = n.putArray("improvements");
        improvements.add("Focus on time complexity analysis").add("Practice without hints");
        n.putArray("sample_answers");
        return n;
    }

    private JsonNode mockPersonalizedRoadmap() {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("dailyRoutine", "1hr DSA + 30min review");
        n.put("weeklyRoutine", "5 problems/week across weak topics");
        n.putArray("phases");
        return n;
    }

    private JsonNode mockQuestionSuggestion() {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("reason", "Practice your weakest topic");
        n.put("topic", "Arrays");
        n.put("difficulty", "medium");
        n.put("focusArea", "General practice");
        return n;
    }

    private JsonNode mockPlacementImprovements() {
        ObjectNode n = objectMapper.createObjectNode();
        var tips = n.putArray("tips");
        tips.add("Complete more interviews for consistency score").add("Target company-specific topics");
        n.put("priority", "Solve 10+ problems across different topics");
        return n;
    }
}
