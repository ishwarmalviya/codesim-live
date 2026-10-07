package com.codesim.controller;

import com.codesim.entity.User;
import com.codesim.repository.UserRepository;
import com.codesim.security.UserPrincipal;
import com.codesim.service.QuestionBrowserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final QuestionBrowserService questionBrowserService;
    private final UserRepository userRepository;

    public SubmissionController(QuestionBrowserService questionBrowserService, UserRepository userRepository) {
        this.questionBrowserService = questionBrowserService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Map<String, Object>> list(@AuthenticationPrincipal UserPrincipal principal,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        return questionBrowserService.getSubmissions(user, page, size);
    }
}
