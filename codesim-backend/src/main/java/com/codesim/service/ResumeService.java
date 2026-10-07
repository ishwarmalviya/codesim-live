package com.codesim.service;

import com.codesim.entity.Resume;
import com.codesim.entity.TopicPerformance;
import com.codesim.entity.User;
import com.codesim.repository.ResumeRepository;
import com.codesim.repository.TopicPerformanceRepository;
import com.codesim.repository.UserCompanyTargetRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class ResumeService {

    private static final List<String> TOPICS = List.of(
            "Arrays", "Strings", "Trees", "Graphs", "Dynamic Programming",
            "Sorting", "Binary Search", "Linked Lists", "Recursion", "Hashing"
    );

    private static final List<String> GENERAL_SKILLS = List.of(
            "Python", "Java", "C++", "JavaScript", "TypeScript", "SQL", "React", "Spring", "Node.js",
            "Django", "Flask", "AWS", "Docker", "Kubernetes", "Machine Learning", "Data Structures",
            "Algorithms", "Git", "REST", "System Design"
    );

    private static final int STARTER_PROFICIENCY = 15;

    private final ResumeRepository resumeRepository;
    private final TopicPerformanceRepository topicPerformanceRepository;
    private final UserCompanyTargetRepository targetRepository;
    private final AiEngineService aiEngineService;
    private final ObjectMapper objectMapper;

    public ResumeService(ResumeRepository resumeRepository, TopicPerformanceRepository topicPerformanceRepository,
                          UserCompanyTargetRepository targetRepository, AiEngineService aiEngineService,
                          ObjectMapper objectMapper) {
        this.resumeRepository = resumeRepository;
        this.topicPerformanceRepository = topicPerformanceRepository;
        this.targetRepository = targetRepository;
        this.aiEngineService = aiEngineService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Resume analyzeAndSave(User user, String rawText) {
        List<String> targetCompanies = targetRepository.findByUserId(user.getId()).stream()
                .map(t -> t.getCompany().getName()).toList();

        JsonNode analysis = aiEngineService.analyzeResume(rawText, targetCompanies);

        String experienceLevel = analysis.path("experienceLevel").asText(fallbackExperienceLevel(rawText));
        List<String> skills = new ArrayList<>();
        analysis.path("technicalSkills").forEach(n -> skills.add(n.asText()));
        if (skills.isEmpty()) {
            skills.addAll(extractKeywordSkills(rawText));
        }

        Resume resume = resumeRepository.findByUserId(user.getId()).orElseGet(() -> {
            Resume r = new Resume();
            r.setUser(user);
            return r;
        });
        resume.setRawText(rawText);
        resume.setParsedSkills(objectMapper.valueToTree(skills).toString());
        resume.setExperienceLevel(experienceLevel);
        resume.setAtsScore(analysis.has("atsScore") ? analysis.get("atsScore").asInt() : null);
        resume.setAnalysisJson(analysis.toString());
        resume.setUploadedAt(Instant.now());
        resumeRepository.save(resume);

        for (String topic : TOPICS) {
            if (rawText.toLowerCase().contains(topic.toLowerCase())) {
                boolean hasPractice = topicPerformanceRepository.findByUserIdAndTopic(user.getId(), topic).isPresent();
                if (!hasPractice) {
                    TopicPerformance tp = new TopicPerformance();
                    tp.setUser(user);
                    tp.setTopic(topic);
                    tp.setProficiency((double) STARTER_PROFICIENCY);
                    tp.setQuestionsAttempted(0);
                    tp.setQuestionsSolved(0);
                    tp.setAvgScore(0.0);
                    tp.setLastPracticed(Instant.now());
                    topicPerformanceRepository.save(tp);
                }
            }
        }

        return resume;
    }

    private String fallbackExperienceLevel(String rawText) {
        String lower = rawText.toLowerCase();
        int years = extractYearsOfExperience(lower);
        if (lower.contains("senior") || lower.contains("lead")) return "Experienced";
        return years >= 3 ? "Experienced" : years >= 1 ? "Junior" : "Fresher";
    }

    private List<String> extractKeywordSkills(String rawText) {
        String lower = rawText.toLowerCase();
        List<String> matched = new ArrayList<>();
        for (String skill : GENERAL_SKILLS) {
            if (lower.contains(skill.toLowerCase())) matched.add(skill);
        }
        return matched;
    }

    private int extractYearsOfExperience(String lower) {
        var matcher = Pattern.compile("(\\d+)\\+?\\s*(year|yrs|yoe)").matcher(lower);
        int max = 0;
        while (matcher.find()) {
            max = Math.max(max, Integer.parseInt(matcher.group(1)));
        }
        return max;
    }

    public Optional<Resume> getResume(Long userId) {
        return resumeRepository.findByUserId(userId);
    }

    public String extractTextFromPdf(byte[] pdfBytes) {
        try (org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.Loader.loadPDF(pdfBytes)) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            return stripper.getText(doc);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse PDF: " + e.getMessage());
        }
    }
}
