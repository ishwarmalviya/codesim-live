package com.codesim.controller;

import com.codesim.dto.InterviewDtos.*;
import com.codesim.entity.User;
import com.codesim.repository.UserRepository;
import com.codesim.security.UserPrincipal;
import com.codesim.service.CodeExecutionService;
import com.codesim.service.InterviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;
    private final UserRepository userRepository;

    public InterviewController(InterviewService interviewService, UserRepository userRepository) {
        this.interviewService = interviewService;
        this.userRepository = userRepository;
    }

    private User currentUser(UserPrincipal principal) {
        return userRepository.findById(principal.getId()).orElseThrow();
    }

    @GetMapping("/topics")
    public List<String> topics() {
        return interviewService.listTopics();
    }

    @PostMapping("/start")
    public InterviewDto start(@AuthenticationPrincipal UserPrincipal principal, @RequestBody StartInterviewRequest req) {
        var interview = interviewService.startInterview(currentUser(principal), req);
        return interviewService.toInterviewDto(interview);
    }

    @GetMapping("/{id}/next")
    public NextQuestionResponse next(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return interviewService.getNextQuestion(id, currentUser(principal));
    }

    @PostMapping("/questions/{iqId}/submit")
    public SubmitResponse submit(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long iqId,
                                  @RequestBody Map<String, Object> body) {
        String code = (String) body.getOrDefault("code", "");
        int timeTaken = ((Number) body.getOrDefault("timeTaken", 0)).intValue();
        String language = (String) body.getOrDefault("language", "python");
        return interviewService.submitAnswer(iqId, currentUser(principal), code, timeTaken, language);
    }

    @PostMapping("/run")
    public CodeExecutionService.PlainRunResult run(@RequestBody RunRequest req) {
        return interviewService.runCode(req.code, req.input, req.args, req.callExpr, req.language);
    }

    @PostMapping("/run-tests")
    public Map<String, Object> runTests(@RequestBody Map<String, Object> body) {
        String code = (String) body.getOrDefault("code", "");
        String language = (String) body.getOrDefault("language", "python");
        Long questionId = ((Number) body.get("questionId")).longValue();
        return Map.of("results", interviewService.runVisibleTests(questionId, code, language));
    }

    @PostMapping("/questions/{iqId}/hint")
    public Map<String, Object> hint(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long iqId,
                                     @RequestBody Map<String, String> body) {
        String code = body.getOrDefault("code", "");
        String hint = interviewService.getHint(iqId, currentUser(principal), code);
        return Map.of("hint", hint);
    }

    @PostMapping("/questions/{iqId}/skip")
    public ResponseEntity<Void> skip(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long iqId) {
        interviewService.skip(iqId, currentUser(principal));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/complete")
    public InterviewDto complete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return interviewService.completeInterview(id, currentUser(principal));
    }

    @GetMapping("/{id}/results")
    public ResultsResponse results(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return interviewService.getResults(id, currentUser(principal));
    }
}
