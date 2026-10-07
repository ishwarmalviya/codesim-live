package com.codesim.controller;

import com.codesim.dto.InterviewDtos.NextQuestionResponse;
import com.codesim.dto.QuestionDto;
import com.codesim.entity.User;
import com.codesim.repository.UserRepository;
import com.codesim.security.UserPrincipal;
import com.codesim.service.InterviewService;
import com.codesim.service.QuestionBrowserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionBrowserService questionBrowserService;
    private final InterviewService interviewService;
    private final UserRepository userRepository;

    public QuestionController(QuestionBrowserService questionBrowserService,
                               InterviewService interviewService,
                               UserRepository userRepository) {
        this.questionBrowserService = questionBrowserService;
        this.interviewService = interviewService;
        this.userRepository = userRepository;
    }

    private User currentUser(UserPrincipal principal) {
        return userRepository.findById(principal.getId()).orElseThrow();
    }

    @GetMapping("/by-topic")
    public Map<String, Object> byTopic(@AuthenticationPrincipal UserPrincipal principal) {
        return questionBrowserService.getQuestionsByTopic(currentUser(principal));
    }

    @GetMapping("/{questionId}")
    public QuestionDto getQuestion(@PathVariable Long questionId) {
        return interviewService.getQuestion(questionId);
    }

    @PostMapping("/{questionId}/practice")
    public NextQuestionResponse startPractice(@AuthenticationPrincipal UserPrincipal principal,
                                               @PathVariable Long questionId) {
        return interviewService.startPractice(currentUser(principal), questionId);
    }

    @GetMapping("/{questionId}/submissions")
    public List<Map<String, Object>> questionSubmissions(@AuthenticationPrincipal UserPrincipal principal,
                                                          @PathVariable Long questionId) {
        return questionBrowserService.getSubmissionsForQuestion(currentUser(principal), questionId);
    }
}
