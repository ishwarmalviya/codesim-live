package com.codesim.service;

import com.codesim.entity.*;
import com.codesim.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Computes company readiness, skill gaps, and a personalized roadmap from a
 * user's topic proficiency + Elo rating against each targeted company's
 * required-topic weights and difficulty threshold.
 */
@Service
public class SkillIntelligenceService {

    private static final int ROADMAP_SIZE = 6;

    private final CompanyRepository companyRepository;
    private final UserCompanyTargetRepository targetRepository;
    private final CompanyReadinessRepository readinessRepository;
    private final SkillGapRepository skillGapRepository;
    private final RoadmapItemRepository roadmapItemRepository;
    private final TopicPerformanceRepository topicPerformanceRepository;
    private final QuestionRepository questionRepository;
    private final CompanyRoadmapRepository companyRoadmapRepository;
    private final AiEngineService aiEngineService;
    private final ObjectMapper objectMapper;
    private final AIReadinessService aiReadinessService;

    public SkillIntelligenceService(CompanyRepository companyRepository,
                                     UserCompanyTargetRepository targetRepository,
                                     CompanyReadinessRepository readinessRepository,
                                     SkillGapRepository skillGapRepository,
                                     RoadmapItemRepository roadmapItemRepository,
                                     TopicPerformanceRepository topicPerformanceRepository,
                                     QuestionRepository questionRepository,
                                     CompanyRoadmapRepository companyRoadmapRepository,
                                     AiEngineService aiEngineService,
                                     ObjectMapper objectMapper,
                                     AIReadinessService aiReadinessService) {
        this.companyRepository = companyRepository;
        this.targetRepository = targetRepository;
        this.readinessRepository = readinessRepository;
        this.skillGapRepository = skillGapRepository;
        this.roadmapItemRepository = roadmapItemRepository;
        this.topicPerformanceRepository = topicPerformanceRepository;
        this.questionRepository = questionRepository;
        this.companyRoadmapRepository = companyRoadmapRepository;
        this.aiEngineService = aiEngineService;
        this.objectMapper = objectMapper;
        this.aiReadinessService = aiReadinessService;
    }

    private Map<String, Double> proficiencyMap(Long userId) {
        Map<String, Double> map = new HashMap<>();
        for (TopicPerformance tp : topicPerformanceRepository.findByUserId(userId)) {
            map.put(tp.getTopic(), tp.getProficiency());
        }
        return map;
    }

    private Map<String, Double> parseWeights(Company company) {
        try {
            JsonNode node = objectMapper.readTree(company.getRequiredTopics());
            Map<String, Double> weights = new LinkedHashMap<>();
            node.fields().forEachRemaining(e -> weights.put(e.getKey(), e.getValue().asDouble()));
            return weights;
        } catch (Exception e) {
            return Map.of();
        }
    }

    @Transactional
    public CompanyReadiness computeReadiness(User user, Company company) {
        Map<String, Double> weights = parseWeights(company);
        Map<String, Double> proficiency = proficiencyMap(user.getId());

        AIReadinessService.ReadinessFactors factors = aiReadinessService.computeFactors(user, company, weights, proficiency);
        double overall = aiReadinessService.computeOverallScore(factors);

        int totalAttempted = factors.questionsAttempted();

        var node = objectMapper.createObjectNode();
        node.put("eloScore", Math.round(factors.eloScore() * 10) / 10.0);
        node.put("topicScore", Math.round(factors.topicCoverage() * 10) / 10.0);
        node.put("topicCoverage", Math.round(factors.topicCoverage() * 10) / 10.0);
        node.put("difficultyProgress", Math.round(factors.difficultyProgress() * 10) / 10.0);
        node.put("accuracy", Math.round(factors.accuracy() * 10) / 10.0);
        node.put("timeEfficiency", Math.round(factors.timeEfficiency() * 10) / 10.0);
        node.put("consistency", Math.round(factors.consistency() * 10) / 10.0);
        node.put("roadmapCompletion", Math.round(factors.roadmapCompletion() * 10) / 10.0);
        node.put("performanceTrend", Math.round(factors.performanceTrend() * 10) / 10.0);
        node.put("userElo", Math.round(user.getEloRating() * 10) / 10.0);
        node.put("requiredElo", company.getDifficultyThreshold());
        node.put("confidence", factors.confidence());
        node.put("questionsAttempted", totalAttempted);

        CompanyReadiness readiness = readinessRepository.findByUserIdAndCompanyId(user.getId(), company.getId())
                .orElseGet(() -> {
                    CompanyReadiness r = new CompanyReadiness();
                    r.setUser(user);
                    r.setCompany(company);
                    return r;
                });
        readiness.setReadinessScore(overall);
        readiness.setBreakdown(node.toString());
        readiness.setComputedAt(Instant.now());
        return readinessRepository.save(readiness);
    }

    @Transactional
    public void recomputeSkillGaps(User user, Company company) {
        Map<String, Double> weights = parseWeights(company);
        Map<String, Double> proficiency = proficiencyMap(user.getId());

        skillGapRepository.deleteByUserId(user.getId());
        List<SkillGap> gaps = new ArrayList<>();
        for (var e : weights.entrySet()) {
            String topic = e.getKey();
            double weight = e.getValue();
            double target = Math.min(100, 40 + weight * 60); // higher-weight topics demand higher target proficiency
            double actual = proficiency.getOrDefault(topic, 0.0);
            double severity = Math.max(0, target - actual);

            SkillGap gap = new SkillGap();
            gap.setUser(user);
            gap.setCompany(company);
            gap.setTopic(topic);
            gap.setRequiredProficiency(Math.round(target * 10) / 10.0);
            gap.setActualProficiency(Math.round(actual * 10) / 10.0);
            gap.setGapSeverity(Math.round(severity * 10) / 10.0);
            gap.setComputedAt(Instant.now());
            gaps.add(gap);
        }
        skillGapRepository.saveAll(gaps);
    }

    @Transactional
    public void recomputeRoadmap(User user) {
        List<SkillGap> gaps = skillGapRepository.findByUserIdOrderByGapSeverityDesc(user.getId());

        roadmapItemRepository.deleteByUserIdAndStatus(user.getId(), "pending");

        int rank = 1;
        List<RoadmapItem> items = new ArrayList<>();
        for (SkillGap gap : gaps.stream().filter(g -> g.getGapSeverity() > 5).limit(ROADMAP_SIZE).toList()) {
            Question suggested = pickQuestionForTopic(user, gap.getTopic());

            RoadmapItem item = new RoadmapItem();
            item.setUser(user);
            item.setTopic(gap.getTopic());
            item.setQuestion(suggested);
            item.setPriorityRank(rank++);
            item.setStatus("pending");
            item.setReason(String.format("%.0f%% gap vs target — practice %s to close it",
                    gap.getGapSeverity(), gap.getTopic()));
            items.add(item);
        }
        roadmapItemRepository.saveAll(items);
    }

    private Question pickQuestionForTopic(User user, String topic) {
        double targetElo = user.getEloRating();
        List<Question> candidates = questionRepository.findCandidates(
                List.of(-1L), targetElo - 300, targetElo + 300, topic);
        if (candidates.isEmpty()) {
            candidates = questionRepository.findBroadCandidates(List.of(-1L), topic, targetElo,
                    org.springframework.data.domain.PageRequest.of(0, 5));
        }
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    @Transactional
    public void recomputeAllForUser(User user) {
        List<UserCompanyTarget> targets = targetRepository.findByUserId(user.getId());
        for (UserCompanyTarget target : targets) {
            computeReadiness(user, target.getCompany());
        }
        // Skill gaps/roadmap are derived from the first targeted company (or a generic
        // "all topics" baseline if the user hasn't picked one yet).
        if (!targets.isEmpty()) {
            recomputeSkillGaps(user, targets.get(0).getCompany());
        } else {
            recomputeGenericSkillGaps(user);
        }
        recomputeRoadmap(user);
    }

    @Transactional
    public void recomputeGenericSkillGaps(User user) {
        Map<String, Double> proficiency = proficiencyMap(user.getId());
        List<String> allTopics = List.of("Arrays", "Strings", "Trees", "Graphs", "Dynamic Programming",
                "Sorting", "Binary Search", "Linked Lists", "Recursion", "Hashing");

        skillGapRepository.deleteByUserId(user.getId());
        List<SkillGap> gaps = new ArrayList<>();
        for (String topic : allTopics) {
            double actual = proficiency.getOrDefault(topic, 0.0);
            double target = 70.0;
            double severity = Math.max(0, target - actual);
            SkillGap gap = new SkillGap();
            gap.setUser(user);
            gap.setCompany(null);
            gap.setTopic(topic);
            gap.setRequiredProficiency(target);
            gap.setActualProficiency(Math.round(actual * 10) / 10.0);
            gap.setGapSeverity(Math.round(severity * 10) / 10.0);
            gaps.add(gap);
        }
        skillGapRepository.saveAll(gaps);
    }

    public List<Company> listCompanies() {
        return companyRepository.findAll();
    }

    @Transactional
    public void addTarget(User user, Long companyId) {
        if (targetRepository.findByUserIdAndCompanyId(user.getId(), companyId).isPresent()) return;
        Company company = companyRepository.findById(companyId).orElseThrow();
        UserCompanyTarget target = new UserCompanyTarget();
        target.setUser(user);
        target.setCompany(company);
        targetRepository.save(target);
        computeReadiness(user, company);
        recomputeSkillGaps(user, company);
        recomputeRoadmap(user);
        generateCompanyAiRoadmap(user, company);
    }

    @Transactional
    public void removeTarget(User user, Long companyId) {
        targetRepository.deleteByUserIdAndCompanyId(user.getId(), companyId);
        companyRoadmapRepository.findByUserIdAndCompanyId(user.getId(), companyId)
                .ifPresent(companyRoadmapRepository::delete);
        recomputeAllForUser(user);
    }

    @Transactional
    public void generateCompanyAiRoadmap(User user, Company company) {
        List<SkillGap> gaps = skillGapRepository.findByUserIdOrderByGapSeverityDesc(user.getId()).stream()
                .filter(g -> g.getCompany() != null && g.getCompany().getId().equals(company.getId()))
                .toList();
        List<String> titles = questionRepository.findDistinctTopics().stream().limit(10).toList();
        try {
            String gapsJson = objectMapper.writeValueAsString(gaps.stream().map(g -> Map.of(
                    "topic", g.getTopic(),
                    "severity", g.getGapSeverity(),
                    "actual", g.getActualProficiency()
            )).toList());
            var roadmap = aiEngineService.generateCompanyRoadmap(user.getName(), company.getName(), gapsJson, titles);
            CompanyRoadmap cr = companyRoadmapRepository.findByUserIdAndCompanyId(user.getId(), company.getId())
                    .orElseGet(() -> {
                        CompanyRoadmap r = new CompanyRoadmap();
                        r.setUser(user);
                        r.setCompany(company);
                        return r;
                    });
            cr.setRoadmapJson(roadmap.toString());
            cr.setGeneratedAt(Instant.now());
            companyRoadmapRepository.save(cr);
        } catch (Exception ignored) {
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCompanyRoadmapMerged(Long userId, Long companyId) {
        Company company = companyRepository.findById(companyId).orElseThrow();
        com.fasterxml.jackson.databind.JsonNode template;
        try {
            String json = company.getRoadmapTemplate();
            if (json != null && !json.isBlank()) {
                template = objectMapper.readTree(json);
            } else {
                template = aiEngineService.mockCompanyRoadmap(company.getName());
            }
        } catch (Exception e) {
            template = aiEngineService.mockCompanyRoadmap(company.getName());
        }
        Optional<CompanyRoadmap> userRoadmap = companyRoadmapRepository.findByUserIdAndCompanyId(userId, companyId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("companyId", companyId);
        result.put("roadmap", template);
        result.put("generatedAt", userRoadmap.map(CompanyRoadmap::getGeneratedAt).orElse(null));
        result.put("source", userRoadmap.isPresent() ? "ai" : "template");
        return result;
    }

    public Optional<CompanyRoadmap> getCompanyRoadmap(Long userId, Long companyId) {
        return companyRoadmapRepository.findByUserIdAndCompanyId(userId, companyId);
    }

    public List<CompanyRoadmap> getCompanyRoadmaps(Long userId) {
        return companyRoadmapRepository.findByUserId(userId);
    }

    public List<UserCompanyTarget> getTargets(Long userId) {
        return targetRepository.findByUserId(userId);
    }

    public List<CompanyReadiness> getReadiness(Long userId) {
        return readinessRepository.findByUserId(userId);
    }

    public List<SkillGap> getSkillGaps(Long userId) {
        return skillGapRepository.findByUserIdOrderByGapSeverityDesc(userId);
    }

    public List<RoadmapItem> getRoadmap(Long userId) {
        return roadmapItemRepository.findByUserIdOrderByPriorityRankAsc(userId);
    }
}
