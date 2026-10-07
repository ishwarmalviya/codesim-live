package com.codesim.controller;

import com.codesim.service.DailyQuestionService;
import com.codesim.service.ContactMessageService;
import com.codesim.repository.InterviewRepository;
import com.codesim.repository.QuestionRepository;
import com.codesim.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final InterviewRepository interviewRepository;
    private final DailyQuestionService dailyQuestionService;
    private final ContactMessageService contactMessageService;

    public PublicController(UserRepository userRepository, QuestionRepository questionRepository,
                             InterviewRepository interviewRepository,
                             DailyQuestionService dailyQuestionService,
                             ContactMessageService contactMessageService) {
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.interviewRepository = interviewRepository;
        this.dailyQuestionService = dailyQuestionService;
        this.contactMessageService = contactMessageService;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return Map.of(
                "totalUsers", userRepository.countByRole("USER"),
                "totalQuestions", questionRepository.countByIsActiveTrue(),
                "totalInterviews", interviewRepository.countByStatus("completed"),
                "topics", questionRepository.findDistinctTopics().size()
        );
    }

    @GetMapping("/daily-question")
    public Map<String, Object> dailyQuestion() {
        return dailyQuestionService.getCurrentPublic();
    }

    @PostMapping("/contact")
    public Map<String, Object> contact(@RequestBody Map<String, String> body) {
        var msg = contactMessageService.submit(
                body.get("name"), body.get("email"), body.get("message"), body.getOrDefault("type", "contact"));
        return Map.of("success", true, "id", msg.getId());
    }
}
