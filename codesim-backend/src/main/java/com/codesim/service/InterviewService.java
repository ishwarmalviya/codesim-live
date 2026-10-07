package com.codesim.service;

import com.codesim.dto.InterviewDtos.*;
import com.codesim.dto.QuestionDto;
import com.codesim.entity.*;
import com.codesim.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final TopicPerformanceRepository topicPerformanceRepository;
    private final AdaptiveEngineService adaptiveEngineService;
    private final CodeExecutionService codeExecutionService;
    private final AiEngineService aiEngineService;
    private final SkillIntelligenceService skillIntelligenceService;
    private final InterviewDnaService interviewDnaService;
    private final PlacementIntelligenceService placementIntelligenceService;
    private final ReplayService replayService;
    private final ObjectMapper objectMapper;

    public InterviewService(InterviewRepository interviewRepository,
                             InterviewQuestionRepository interviewQuestionRepository,
                             QuestionRepository questionRepository,
                             UserRepository userRepository,
                             CompanyRepository companyRepository,
                             TopicPerformanceRepository topicPerformanceRepository,
                             AdaptiveEngineService adaptiveEngineService,
                             CodeExecutionService codeExecutionService,
                             AiEngineService aiEngineService,
                             SkillIntelligenceService skillIntelligenceService,
                             InterviewDnaService interviewDnaService,
                             PlacementIntelligenceService placementIntelligenceService,
                             ReplayService replayService,
                             ObjectMapper objectMapper) {
        this.interviewRepository = interviewRepository;
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.topicPerformanceRepository = topicPerformanceRepository;
        this.adaptiveEngineService = adaptiveEngineService;
        this.codeExecutionService = codeExecutionService;
        this.aiEngineService = aiEngineService;
        this.skillIntelligenceService = skillIntelligenceService;
        this.interviewDnaService = interviewDnaService;
        this.placementIntelligenceService = placementIntelligenceService;
        this.replayService = replayService;
        this.objectMapper = objectMapper;
    }

    public List<String> listTopics() {
        return questionRepository.findDistinctTopics();
    }

    @Transactional(readOnly = true)
    public QuestionDto getQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        return toQuestionDto(question);
    }

    /**
     * Opens a single-question practice session (not a timed mock interview).
     */
    @Transactional
    public NextQuestionResponse startPractice(User user, Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

        Interview interview = new Interview();
        interview.setUser(user);
        interview.setInterviewType("practice");
        interview.setSelectedTopic(question.getTopic());
        interview.setTotalQuestions(1);
        interview.setEloBefore(user.getEloRating());
        interview = interviewRepository.save(interview);

        InterviewQuestion iq = new InterviewQuestion();
        iq.setInterview(interview);
        iq.setQuestion(question);
        iq.setQuestionOrder(1);
        iq.setTestsTotal(countTestCases(question));
        interviewQuestionRepository.save(iq);

        NextQuestionResponse resp = new NextQuestionResponse();
        resp.completed = false;
        resp.interviewQuestionId = iq.getId();
        resp.questionNumber = 1;
        resp.totalQuestions = 1;
        resp.question = toQuestionDto(question);
        resp.timeLimitSeconds = null;
        return resp;
    }

    @Transactional
    public Interview startInterview(User user, StartInterviewRequest req) {
        int numQuestions = Math.max(3, Math.min(10, req.numQuestions));
        Interview interview = new Interview();
        interview.setUser(user);
        interview.setInterviewType(req.type);
        interview.setSelectedTopic(req.topic);
        interview.setTotalQuestions(numQuestions);
        interview.setEloBefore(user.getEloRating());
        if (req.topics != null && !req.topics.isEmpty()) {
            try {
                interview.setSelectedTopics(objectMapper.writeValueAsString(req.topics));
            } catch (Exception ignored) {
                interview.setSelectedTopics("[]");
            }
        }
        if (req.targetCompanyId != null) {
            interview.setTargetCompanyId(req.targetCompanyId);
        }
        if (req.targetRole != null) {
            interview.setTargetRole(req.targetRole);
        }
        return interviewRepository.save(interview);
    }

    private Interview loadOwnedInterview(Long interviewId, User user) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview not found"));
        if (!interview.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your interview");
        }
        return interview;
    }

    @Transactional
    public NextQuestionResponse getNextQuestion(Long interviewId, User user) {
        Interview interview = loadOwnedInterview(interviewId, user);
        NextQuestionResponse resp = new NextQuestionResponse();

        if (!interview.getStatus().equals("active")) {
            resp.completed = true;
            return resp;
        }

        List<InterviewQuestion> answered = interviewQuestionRepository.findByInterviewIdOrderByQuestionOrderAsc(interviewId);
        int questionNum = answered.size() + 1;

        if (questionNum > interview.getTotalQuestions()) {
            resp.completed = true;
            return resp;
        }

        // If the last question is still pending (no submission), return it again instead of picking a new one
        if (!answered.isEmpty()) {
            InterviewQuestion last = answered.get(answered.size() - 1);
            if (last.getStatus().equals("pending")) {
                resp.interviewQuestionId = last.getId();
                resp.questionNumber = last.getQuestionOrder();
                resp.totalQuestions = interview.getTotalQuestions();
                resp.question = toQuestionDto(last.getQuestion());
                return resp;
            }
        }

        List<Long> answeredIds = answered.stream().map(iq -> iq.getQuestion().getId()).collect(Collectors.toList());
        String topicFilter = interview.getSelectedTopic();
        List<String> topicList = parseTopicList(interview.getSelectedTopics());
        Double companyElo = null;
        if (interview.getTargetCompanyId() != null) {
            companyElo = companyRepository.findById(interview.getTargetCompanyId())
                    .map(Company::getDifficultyThreshold).orElse(null);
        }

        var questionOpt = adaptiveEngineService.selectNextQuestionWithFallback(user, answeredIds, topicFilter, topicList, companyElo);
        if (questionOpt.isEmpty() && questionNum <= interview.getTotalQuestions()) {
            // Last resort: allow revisiting a question from this interview if bank is exhausted
            questionOpt = questionRepository.findBroadCandidates(List.of(-1L), null, user.getEloRating(), org.springframework.data.domain.PageRequest.of(0, 1)).stream().findFirst();
        }
        if (questionOpt.isEmpty()) {
            resp.completed = true;
            return resp;
        }

        Question question = questionOpt.get();
        int testsTotal = countTestCases(question);

        InterviewQuestion iq = new InterviewQuestion();
        iq.setInterview(interview);
        iq.setQuestion(question);
        iq.setQuestionOrder(questionNum);
        iq.setTestsTotal(testsTotal);
        interviewQuestionRepository.save(iq);

        resp.interviewQuestionId = iq.getId();
        resp.questionNumber = questionNum;
        resp.totalQuestions = interview.getTotalQuestions();
        resp.question = toQuestionDto(question);
        return resp;
    }

    private int countTestCases(Question question) {
        try {
            JsonNode arr = objectMapper.readTree(question.getTestCases());
            return arr.isArray() ? arr.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private List<String> parseTopicList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isArray()) return List.of();
            List<String> out = new ArrayList<>();
            node.forEach(n -> out.add(n.asText()));
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

    @Transactional
    public SubmitResponse submitAnswer(Long iqId, User user, String code, int timeTaken, String language) {
        InterviewQuestion iq = interviewQuestionRepository.findById(iqId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        if (!iq.getInterview().getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your interview");
        }

        Question question = iq.getQuestion();
        boolean isResubmit = iq.getSubmittedAt() != null;
        boolean wasSolved = "solved".equals(iq.getStatus());

        List<JsonNode> testCases = new ArrayList<>();
        try {
            objectMapper.readTree(question.getTestCases()).forEach(testCases::add);
        } catch (Exception ignored) {
        }

        String lang = language != null && !language.isBlank() ? language : "python";
        var testsFuture = java.util.concurrent.CompletableFuture.supplyAsync(
                () -> codeExecutionService.runTests(code, testCases, lang));
        var analysisFuture = java.util.concurrent.CompletableFuture.supplyAsync(
                () -> codeExecutionService.analyze(code));
        var execResults = testsFuture.join();
        JsonNode analysisNode = analysisFuture.join();
        JsonNode analysis = analysisNode != null ? analysisNode.path("analysis") : null;

        double score = adaptiveEngineService.calculateQuestionScore(
                execResults.passed(), execResults.total(), timeTaken, iq.getHintsUsed(), 1800);

        if (isResubmit) {
            user.setEloRating(user.getEloRating() - iq.getUserEloDelta());
            question.setEloRating(question.getEloRating() - iq.getQuestionEloDelta());
        }

        iq.setUserCode(code);
        iq.setScore(score);
        iq.setTestsPassed(execResults.passed());
        iq.setTestsTotal(execResults.total());
        iq.setTimeTakenSeconds(timeTaken);
        iq.setSubmittedAt(Instant.now());
        iq.setCodeQualityScore(analysis != null ? analysis.path("code_quality_score").asDouble(0) : 0);
        iq.setComplexityAnalysis(analysisNode != null ? analysisNode.path("complexity").toString() : "{}");
        iq.setStatus(execResults.allPassed() ? "solved" : "failed");

        if (!isResubmit) {
            question.setTimesAttempted(question.getTimesAttempted() + 1);
            if (execResults.allPassed()) {
                question.setTimesSolved(question.getTimesSolved() + 1);
            }
        } else {
            if (wasSolved && !execResults.allPassed()) {
                question.setTimesSolved(Math.max(0, question.getTimesSolved() - 1));
            } else if (!wasSolved && execResults.allPassed()) {
                question.setTimesSolved(question.getTimesSolved() + 1);
            }
        }

        double oldUserElo = user.getEloRating();
        double oldQuestionElo = question.getEloRating();
        double[] newElos = adaptiveEngineService.updateRatingsAfterQuestion(user, question, score, 10.0);
        iq.setUserEloDelta(newElos[0] - oldUserElo);
        iq.setQuestionEloDelta(newElos[1] - oldQuestionElo);
        user.setEloRating(newElos[0]);
        question.setEloRating(newElos[1]);

        if (isResubmit) {
            adaptiveEngineService.updateTopicPerformanceOnResubmit(
                    user, question.getTopic(), execResults.allPassed(), score, wasSolved);
        } else {
            adaptiveEngineService.updateTopicPerformance(user, question.getTopic(), execResults.allPassed(), score);
        }

        JsonNode review = aiEngineService.getCodeReview(
                question.getTitle(), question.getDescription(), code,
                execResults.passed(), execResults.total(), lang);
        iq.setAiFeedback(review.toString());

        userRepository.save(user);
        questionRepository.save(question);
        interviewQuestionRepository.save(iq);

        // Recompute readiness for targeted companies after each submission
        skillIntelligenceService.recomputeAllForUser(user);

        var eventPayload = objectMapper.createObjectNode();
        eventPayload.put("testsPassed", execResults.passed());
        eventPayload.put("testsTotal", execResults.total());
        eventPayload.put("score", score);
        eventPayload.put("resubmit", isResubmit);
        replayService.recordEvent(iq, "submit", code, eventPayload);

        SubmitResponse resp = new SubmitResponse();
        resp.success = true;
        resp.testResults = new ArrayList<>();
        int i = 1;
        for (var r : execResults.results()) {
            var dto = new TestResultDto();
            dto.testNum = i++;
            dto.passed = r.passed();
            dto.description = r.description();
            dto.error = r.error();
            dto.expected = r.hidden() ? "(hidden)" : null;
            dto.actual = r.hidden() ? "(hidden)" : r.actual();
            dto.hidden = r.hidden();
            resp.testResults.add(dto);
        }
        resp.passed = execResults.passed();
        resp.total = execResults.total();
        resp.allPassed = execResults.allPassed();
        resp.score = score;
        resp.maxScore = iq.getMaxScore();
        resp.eloChange = Math.round((newElos[0] - iq.getInterview().getEloBefore()) * 10) / 10.0;
        resp.review = review;
        resp.analysis = analysisNode;
        return resp;
    }

    public com.codesim.service.CodeExecutionService.PlainRunResult runCode(
            String code, String input, List<Object> args, String callExpr, String language) {
        String lang = language != null && !language.isBlank() ? language : "python";
        if (args != null && !args.isEmpty()) {
            return codeExecutionService.runWithSample(code, args, callExpr, lang);
        }
        return codeExecutionService.runPlain(code, input, lang);
    }

    public List<Map<String, Object>> runVisibleTests(Long questionId, String code, String language) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        List<com.fasterxml.jackson.databind.JsonNode> testCases = new ArrayList<>();
        try {
            com.fasterxml.jackson.databind.JsonNode all = objectMapper.readTree(question.getTestCases());
            if (all.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode tc : all) {
                    if (!(tc.has("hidden") && tc.get("hidden").asBoolean())) {
                        testCases.add(tc);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        String lang = language != null && !language.isBlank() ? language : "python";
        var execResults = codeExecutionService.runTests(code, testCases, lang);
        List<Map<String, Object>> results = new ArrayList<>();
        int i = 1;
        for (var r : execResults.results()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("testNum", i++);
            m.put("passed", r.passed());
            m.put("description", r.description());
            m.put("error", r.error());
            m.put("stdout", r.actual());
            results.add(m);
        }
        return results;
    }

    @Transactional
    public String getHint(Long iqId, User user, String code) {
        InterviewQuestion iq = interviewQuestionRepository.findById(iqId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        if (!iq.getInterview().getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your interview");
        }
        iq.setHintsUsed(iq.getHintsUsed() + 1);
        interviewQuestionRepository.save(iq);

        Question question = iq.getQuestion();
        String hint;
        try {
            JsonNode storedHints = objectMapper.readTree(question.getHints());
            boolean useAiFirst = aiEngineService.hasApiKey();
            if (useAiFirst) {
                hint = aiEngineService.getHint(question.getTitle(), question.getDescription(), code, iq.getHintsUsed());
            } else if (storedHints.isArray() && iq.getHintsUsed() <= storedHints.size()) {
                hint = storedHints.get(iq.getHintsUsed() - 1).asText();
            } else {
                hint = aiEngineService.getHint(question.getTitle(), question.getDescription(), code, iq.getHintsUsed());
            }
        } catch (Exception e) {
            hint = aiEngineService.getHint(question.getTitle(), question.getDescription(), code, iq.getHintsUsed());
        }

        var eventPayload = objectMapper.createObjectNode();
        eventPayload.put("hintNumber", iq.getHintsUsed());
        eventPayload.put("hint", hint);
        replayService.recordEvent(iq, "hint", code, eventPayload);

        return hint;
    }

    @Transactional
    public void skip(Long iqId, User user) {
        InterviewQuestion iq = interviewQuestionRepository.findById(iqId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        if (!iq.getInterview().getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your interview");
        }
        iq.setStatus("skipped");
        iq.setSubmittedAt(Instant.now());
        interviewQuestionRepository.save(iq);
    }

    @Transactional
    public InterviewDto completeInterview(Long interviewId, User user) {
        Interview interview = loadOwnedInterview(interviewId, user);

        if (interview.getStatus().equals("active")) {
            interview.setStatus("completed");
            interview.setCompletedAt(Instant.now());

            List<InterviewQuestion> iqs = interviewQuestionRepository.findByInterviewIdOrderByQuestionOrderAsc(interviewId);
            double totalScore = iqs.stream().mapToDouble(InterviewQuestion::getScore).sum();
            double maxScore = iqs.stream().mapToDouble(InterviewQuestion::getMaxScore).sum();
            interview.setScore(totalScore);
            interview.setMaxScore(maxScore);
            interview.setQuestionsAnswered(iqs.size());
            interview.setEloAfter(user.getEloRating());

            user.setTotalInterviews(user.getTotalInterviews() + 1);
            long solved = iqs.stream().filter(iq -> iq.getStatus().equals("solved")).count();
            user.setTotalQuestionsSolved((int) (user.getTotalQuestionsSolved() + solved));

            long completedCount = interviewRepository.countByUserIdAndStatus(user.getId(), "completed") + 1;
            double pct = interview.getScorePercent();
            double prevAvg = user.getAvgScore() * (completedCount - 1);
            user.setAvgScore((prevAvg + pct) / completedCount);

            if ("assessment".equals(interview.getInterviewType())) {
                user.setHasCompletedAssessment(true);
            }

            userRepository.save(user);
            interviewRepository.save(interview);

            List<InterviewQuestion> iqsForAi = interviewQuestionRepository
                    .findByInterviewIdOrderByQuestionOrderAsc(interviewId);
            JsonNode aiSummary = aiEngineService.generateMockInterviewFeedback(interview, iqsForAi, user);
            interview.setAiSummary(aiSummary.toString());
            interviewRepository.save(interview);

            // Continuous improvement loop: recompute derived analytics after every completed interview.
            skillIntelligenceService.recomputeAllForUser(user);
            interviewDnaService.recompute(user);
            placementIntelligenceService.recompute(user);
        }

        return toInterviewDto(interview);
    }

    @Transactional(readOnly = true)
    public ResultsResponse getResults(Long interviewId, User user) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview not found"));
        if (!interview.getUser().getId().equals(user.getId()) && !user.getRole().equals("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your interview");
        }

        List<InterviewQuestion> iqs = interviewQuestionRepository.findByInterviewIdOrderByQuestionOrderAsc(interviewId);

        ResultsResponse resp = new ResultsResponse();
        resp.interview = toInterviewDto(interview);
        resp.eloChange = Math.round((interview.getEloAfter() - interview.getEloBefore()) * 10) / 10.0;
        try {
            resp.aiSummary = interview.getAiSummary() != null && !interview.getAiSummary().isBlank()
                    ? objectMapper.readTree(interview.getAiSummary()) : null;
        } catch (Exception e) {
            resp.aiSummary = null;
        }
        resp.questions = iqs.stream().map(iq -> {
            var dto = new QuestionResultDto();
            dto.iqId = iq.getId();
            dto.question = toQuestionDto(iq.getQuestion());
            dto.userCode = iq.getUserCode();
            dto.score = iq.getScore();
            dto.maxScore = iq.getMaxScore();
            dto.testsPassed = iq.getTestsPassed();
            dto.testsTotal = iq.getTestsTotal();
            dto.hintsUsed = iq.getHintsUsed();
            dto.status = iq.getStatus();
            try {
                dto.review = iq.getAiFeedback() != null && !iq.getAiFeedback().isBlank()
                        ? objectMapper.readTree(iq.getAiFeedback()) : null;
            } catch (Exception e) {
                dto.review = null;
            }
            return dto;
        }).collect(Collectors.toList());

        return resp;
    }

    public QuestionDto toQuestionDto(Question q) {
        QuestionDto dto = new QuestionDto();
        dto.id = q.getId();
        dto.title = q.getTitle();
        dto.description = q.getDescription();
        dto.difficulty = q.getDifficulty();
        dto.topic = q.getTopic();
        dto.subtopic = q.getSubtopic();
        dto.eloRating = Math.round(q.getEloRating() * 10) / 10.0;
        dto.starterCode = q.getStarterCode();
        try {
            JsonNode langs = objectMapper.readTree(q.getSupportedLanguages());
            dto.supportedLanguages = new ArrayList<>();
            if (langs.isArray()) langs.forEach(n -> dto.supportedLanguages.add(n.asText()));
            else dto.supportedLanguages = List.of("python");
        } catch (Exception e) {
            dto.supportedLanguages = List.of("python");
        }
        try {
            dto.starterCodeByLang = objectMapper.readTree(q.getStarterCodeByLang());
        } catch (Exception e) {
            dto.starterCodeByLang = objectMapper.createObjectNode();
        }
        dto.functionSignature = q.getFunctionSignature();
        dto.constraints = q.getConstraints();
        dto.examples = q.getExamples();
        dto.timeComplexity = q.getTimeComplexity();
        dto.spaceComplexity = q.getSpaceComplexity();
        dto.tags = q.getTags() == null || q.getTags().isBlank() ? List.of() : List.of(q.getTags().split(","));
        dto.successRate = q.getSuccessRate();
        dto.timesAttempted = q.getTimesAttempted();
        try {
            dto.hints = objectMapper.readTree(q.getHints());
        } catch (Exception e) {
            dto.hints = objectMapper.createArrayNode();
        }
        try {
            JsonNode all = objectMapper.readTree(q.getTestCases());
            var visible = objectMapper.createArrayNode();
            if (all.isArray()) {
                for (JsonNode tc : all) {
                    if (!(tc.has("hidden") && tc.get("hidden").asBoolean())) {
                        visible.add(tc);
                    }
                }
            }
            dto.testCases = visible;
        } catch (Exception e) {
            dto.testCases = objectMapper.createArrayNode();
        }
        return dto;
    }

    public InterviewDto toInterviewDto(Interview i) {
        InterviewDto dto = new InterviewDto();
        dto.id = i.getId();
        dto.status = i.getStatus();
        dto.interviewType = i.getInterviewType();
        dto.selectedTopic = i.getSelectedTopic();
        dto.totalQuestions = i.getTotalQuestions();
        dto.questionsAnswered = i.getQuestionsAnswered();
        dto.score = Math.round(i.getScore() * 10) / 10.0;
        dto.maxScore = i.getMaxScore();
        dto.scorePercent = i.getScorePercent();
        dto.eloBefore = Math.round(i.getEloBefore() * 10) / 10.0;
        dto.eloAfter = Math.round(i.getEloAfter() * 10) / 10.0;
        dto.startedAt = i.getStartedAt();
        dto.completedAt = i.getCompletedAt();
        dto.durationMinutes = i.getDurationMinutes();
        return dto;
    }
}
