package com.codesim.config;

import com.codesim.entity.Company;
import com.codesim.entity.Question;
import com.codesim.entity.User;
import com.codesim.repository.CompanyRepository;
import com.codesim.repository.QuestionRepository;
import com.codesim.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Map;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Map<String, Double> ELO_MAP = Map.of(
            "easy", 900.0, "medium", 1200.0, "hard", 1550.0, "expert", 1800.0
    );

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public DataSeeder(UserRepository userRepository, QuestionRepository questionRepository,
                       CompanyRepository companyRepository, PasswordEncoder passwordEncoder,
                       ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        seedUsers();
        seedQuestions();
        seedCompanies();
        supplementHiddenTestCases();
    }

    private void seedUsers() {
        if (!userRepository.existsByEmail("admin@codesim.ai")) {
            User admin = new User();
            admin.setName("Admin");
            admin.setEmail("admin@codesim.ai");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            userRepository.save(admin);
        }
        if (!userRepository.existsByEmail("demo@codesim.ai")) {
            User demo = new User();
            demo.setName("Demo User");
            demo.setEmail("demo@codesim.ai");
            demo.setPasswordHash(passwordEncoder.encode("demo123"));
            demo.setRole("USER");
            demo.setCollege("Demo Institute of Technology");
            userRepository.save(demo);
        }
        if (!userRepository.existsByEmail("recruiter@codesim.ai")) {
            User recruiter = new User();
            recruiter.setName("Demo Recruiter");
            recruiter.setEmail("recruiter@codesim.ai");
            recruiter.setPasswordHash(passwordEncoder.encode("recruiter123"));
            recruiter.setRole("RECRUITER");
            userRepository.save(recruiter);
        }
    }

    private void seedQuestions() {
        if (questionRepository.count() > 0) return;

        try (InputStream in = new ClassPathResource("seed/questions_seed.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(in);
            for (JsonNode qn : root) {
                Question q = new Question();
                q.setTitle(qn.path("title").asText());
                q.setTopic(qn.path("topic").asText());
                q.setSubtopic(qn.path("subtopic").asText(""));
                String difficulty = qn.path("difficulty").asText("medium");
                q.setDifficulty(difficulty);
                q.setEloRating(ELO_MAP.getOrDefault(difficulty, 1200.0));
                q.setDescription(qn.path("description").asText(""));
                q.setExamples(qn.path("examples").asText(""));
                q.setConstraints(qn.path("constraints").asText(""));
                q.setStarterCode(qn.path("starter_code").asText(""));
                q.setSolutionCode(qn.path("solution_code").asText(""));
                q.setFunctionSignature(qn.path("function_signature").asText(""));
                q.setTestCases(qn.path("test_cases").toString());
                q.setHints(qn.path("hints").toString());
                q.setTimeComplexity(qn.path("time_complexity").asText(""));
                q.setSpaceComplexity(qn.path("space_complexity").asText(""));
                q.setTags(qn.path("tags").asText(""));
                q.setIsActive(true);
                questionRepository.save(q);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed questions", e);
        }
    }

    private void seedCompanies() {
        if (companyRepository.count() > 0) {
            // Backfill roadmap templates for existing companies
            for (Company c : companyRepository.findAll()) {
                if (c.getRoadmapTemplate() == null || c.getRoadmapTemplate().isBlank()) {
                    c.setRoadmapTemplate(buildRoadmapTemplate(c.getName(), c.getRequiredTopics()));
                    companyRepository.save(c);
                }
            }
            return;
        }

        try (InputStream in = new ClassPathResource("seed/companies_seed.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(in);
            for (JsonNode cn : root) {
                Company c = new Company();
                c.setName(cn.path("name").asText());
                c.setLogoEmoji(cn.path("logoEmoji").asText("🏢"));
                c.setDifficultyThreshold(cn.path("difficultyThreshold").asDouble(1200.0));
                c.setTags(cn.path("tags").asText(""));
                String topics = cn.path("requiredTopics").toString();
                c.setRequiredTopics(topics);
                c.setRoadmapTemplate(buildRoadmapTemplate(c.getName(), topics));
                companyRepository.save(c);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed companies", e);
        }
    }

    private String buildRoadmapTemplate(String companyName, String requiredTopicsJson) {
        try {
            JsonNode topics = objectMapper.readTree(requiredTopicsJson);
            var root = objectMapper.createObjectNode();
            root.put("skillGapAnalysis", "Complete interview prep roadmap for " + companyName
                    + ". Focus on high-weight DSA topics and consistent daily practice.");
            var dsa = root.putArray("dsaRoadmap");
            int priority = 1;
            var fields = new java.util.ArrayList<Map.Entry<String, JsonNode>>();
            topics.fields().forEachRemaining(fields::add);
            fields.sort((a, b) -> Double.compare(b.getValue().asDouble(), a.getValue().asDouble()));
            for (var e : fields) {
                var item = dsa.addObject();
                item.put("topic", e.getKey());
                item.put("priority", priority++);
                item.put("description", "Master " + e.getKey() + " — weight " + Math.round(e.getValue().asDouble() * 100) + "% for " + companyName);
            }
            var core = root.putArray("coreSubjects");
            var os = core.addObject();
            os.put("subject", "OS");
            os.put("priority", 1);
            os.putArray("topics").add("Processes").add("Scheduling").add("Memory");
            var weekly = root.putArray("weeklyPlan");
            weekly.add("Week 1-2: Foundation topics + 2 problems/day");
            weekly.add("Week 3-4: Medium problems on priority topics");
            weekly.add("Week 5-6: Mock interviews + weak topic review");
            var rec = root.putArray("recommendedQuestions");
            rec.add("Start with easy " + (fields.isEmpty() ? "Arrays" : fields.get(0).getKey()) + " problems");
            rec.add("Progress to medium difficulty on weak topics");
            rec.add("Complete company-specific mock interview");
            return root.toString();
        } catch (Exception e) {
            return aiEngineServiceFallback(companyName);
        }
    }

    private String aiEngineServiceFallback(String companyName) {
        return "{\"skillGapAnalysis\":\"Roadmap for " + companyName + "\",\"dsaRoadmap\":[{\"topic\":\"Arrays\",\"priority\":1,\"description\":\"Foundation\"}],\"weeklyPlan\":[\"Practice daily\"],\"recommendedQuestions\":[\"Arrays basics\"]}";
    }

    private void supplementHiddenTestCases() {
        for (Question q : questionRepository.findAll()) {
            try {
                JsonNode arr = objectMapper.readTree(q.getTestCases());
                if (!arr.isArray() || arr.isEmpty()) continue;
                int hidden = 0;
                JsonNode template = null;
                for (JsonNode tc : arr) {
                    if (tc.has("hidden") && tc.get("hidden").asBoolean()) {
                        hidden++;
                    } else if (template == null) {
                        template = tc;
                    }
                }
                if (hidden >= 3 || template == null) continue;

                var newArr = objectMapper.createArrayNode();
                arr.forEach(newArr::add);

                String[] labels = {"Hidden edge: boundary", "Hidden edge: alternate", "Hidden edge: stress"};
                for (int i = hidden; i < 3; i++) {
                    var tc = template.deepCopy();
                    if (tc.isObject()) {
                        ((com.fasterxml.jackson.databind.node.ObjectNode) tc).put("description", labels[i]);
                        ((com.fasterxml.jackson.databind.node.ObjectNode) tc).put("hidden", true);
                    }
                    newArr.add(tc);
                }
                q.setTestCases(newArr.toString());
                questionRepository.save(q);
            } catch (Exception ignored) {
            }
        }
    }
}
