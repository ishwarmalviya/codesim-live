package com.codesim.service;

import com.codesim.entity.InterviewQuestion;
import com.codesim.entity.Question;
import com.codesim.entity.User;
import com.codesim.repository.InterviewQuestionRepository;
import com.codesim.repository.QuestionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuestionBrowserService {

    private final QuestionRepository questionRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;

    public QuestionBrowserService(QuestionRepository questionRepository,
                                   InterviewQuestionRepository interviewQuestionRepository) {
        this.questionRepository = questionRepository;
        this.interviewQuestionRepository = interviewQuestionRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getQuestionsByTopic(User user) {
        Map<Long, String> bestStatus = buildStatusMap(user.getId());
        List<String> topics = questionRepository.findDistinctTopics();
        List<Map<String, Object>> topicList = new ArrayList<>();

        for (String topic : topics) {
            List<Question> questions = questionRepository.findBroadCandidates(List.of(-1L), topic, 1200.0, PageRequest.of(0, 200));
            int solved = 0;
            List<Map<String, Object>> qList = new ArrayList<>();
            for (Question q : questions) {
                String status = bestStatus.getOrDefault(q.getId(), "unsolved");
                if ("solved".equals(status)) solved++;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", q.getId());
                item.put("title", q.getTitle());
                item.put("difficulty", q.getDifficulty());
                item.put("topic", q.getTopic());
                item.put("status", status);
                item.put("acceptanceRate", q.getSuccessRate());
                item.put("eloRating", q.getEloRating());
                qList.add(item);
            }
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("name", topic);
            t.put("total", qList.size());
            t.put("solved", solved);
            t.put("questions", qList);
            topicList.add(t);
        }
        return Map.of("topics", topicList);
    }

    private Map<Long, String> buildStatusMap(Long userId) {
        Map<Long, String> map = new HashMap<>();
        for (InterviewQuestion iq : interviewQuestionRepository.findAllSubmittedByUser(userId)) {
            Long qid = iq.getQuestion().getId();
            String current = map.get(qid);
            if ("solved".equals(iq.getStatus())) {
                map.put(qid, "solved");
            } else if (!"solved".equals(current)) {
                map.put(qid, "attempted");
            }
        }
        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSubmissions(User user, int page, int size) {
        List<InterviewQuestion> all = interviewQuestionRepository.findAllSubmittedByUser(user.getId());
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(all.size(), from + size);
        if (from >= all.size()) return List.of();

        List<InterviewQuestion> slice = all.subList(from, to);
        Collections.reverse(slice);
        return slice.stream().map(this::toSubmissionDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSubmissionsForQuestion(User user, Long questionId) {
        return interviewQuestionRepository.findAllSubmittedByUser(user.getId()).stream()
                .filter(iq -> iq.getQuestion().getId().equals(questionId))
                .map(this::toSubmissionDto)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toSubmissionDto(InterviewQuestion iq) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", iq.getId());
        m.put("questionId", iq.getQuestion().getId());
        m.put("questionTitle", iq.getQuestion().getTitle());
        m.put("topic", iq.getQuestion().getTopic());
        m.put("difficulty", iq.getQuestion().getDifficulty());
        m.put("status", mapStatus(iq));
        m.put("statusLabel", statusLabel(iq));
        m.put("testsPassed", iq.getTestsPassed());
        m.put("testsTotal", iq.getTestsTotal());
        m.put("score", iq.getScore());
        m.put("submittedAt", iq.getSubmittedAt());
        m.put("code", iq.getUserCode());
        m.put("timeTakenSeconds", iq.getTimeTakenSeconds());
        return m;
    }

    private String mapStatus(InterviewQuestion iq) {
        if ("solved".equals(iq.getStatus())) return "accepted";
        if ("skipped".equals(iq.getStatus())) return "skipped";
        if (iq.getTestsPassed() == 0 && iq.getTestsTotal() > 0) return "wrong_answer";
        return "failed";
    }

    private String statusLabel(InterviewQuestion iq) {
        return switch (mapStatus(iq)) {
            case "accepted" -> "Accepted";
            case "skipped" -> "Skipped";
            default -> "Wrong Answer";
        };
    }
}
