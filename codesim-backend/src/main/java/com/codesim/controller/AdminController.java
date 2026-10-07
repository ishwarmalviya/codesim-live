package com.codesim.controller;

import com.codesim.dto.AdminDtos.*;
import com.codesim.dto.QuestionDto;
import com.codesim.entity.Question;
import com.codesim.entity.User;
import com.codesim.repository.UserRepository;
import com.codesim.security.UserPrincipal;
import com.codesim.service.AdminService;
import com.codesim.service.ContactMessageService;
import com.codesim.service.DailyQuestionService;
import com.codesim.service.InterviewService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final InterviewService interviewService;
    private final DailyQuestionService dailyQuestionService;
    private final ContactMessageService contactMessageService;
    private final UserRepository userRepository;

    public AdminController(AdminService adminService, InterviewService interviewService,
                            DailyQuestionService dailyQuestionService,
                            ContactMessageService contactMessageService,
                            UserRepository userRepository) {
        this.adminService = adminService;
        this.interviewService = interviewService;
        this.dailyQuestionService = dailyQuestionService;
        this.contactMessageService = contactMessageService;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return adminService.getDashboard();
    }

    @GetMapping("/analytics")
    public AdminAnalyticsResponse analytics() {
        return adminService.getAnalytics();
    }

    @GetMapping("/users")
    public Page<AdminUserDto> users(@RequestParam(required = false) String search,
                                     @RequestParam(defaultValue = "1") int page) {
        return adminService.searchUsers(search, page).map(this::toAdminUserDto);
    }

    @PostMapping("/users/{id}/toggle")
    public AdminUserDto toggleUser(@PathVariable Long id) {
        return toAdminUserDto(adminService.toggleUser(id));
    }

    @GetMapping("/questions")
    public Page<QuestionDto> questions(@RequestParam(required = false) String search,
                                        @RequestParam(required = false) String difficulty,
                                        @RequestParam(required = false) String topic,
                                        @RequestParam(defaultValue = "1") int page) {
        return adminService.searchQuestions(search, difficulty, topic, page).map(interviewService::toQuestionDto);
    }

    @GetMapping("/questions/{id}")
    public AdminQuestionDetailDto getQuestion(@PathVariable Long id) {
        Question q = adminService.getQuestion(id);
        AdminQuestionDetailDto dto = new AdminQuestionDetailDto();
        dto.id = q.getId();
        dto.title = q.getTitle();
        dto.description = q.getDescription();
        dto.difficulty = q.getDifficulty();
        dto.topic = q.getTopic();
        dto.subtopic = q.getSubtopic();
        dto.starterCode = q.getStarterCode();
        dto.solutionCode = q.getSolutionCode();
        dto.functionSignature = q.getFunctionSignature();
        dto.testCases = q.getTestCases();
        dto.hints = q.getHints();
        dto.constraints = q.getConstraints();
        dto.examples = q.getExamples();
        dto.timeComplexity = q.getTimeComplexity();
        dto.spaceComplexity = q.getSpaceComplexity();
        dto.tags = q.getTags();
        return dto;
    }

    @PostMapping("/questions")
    public QuestionDto createQuestion(@AuthenticationPrincipal UserPrincipal principal,
                                       @RequestBody QuestionUpsertRequest req) {
        Question q = adminService.saveQuestion(null, req, principal.getId());
        return interviewService.toQuestionDto(q);
    }

    @PutMapping("/questions/{id}")
    public QuestionDto updateQuestion(@AuthenticationPrincipal UserPrincipal principal,
                                       @PathVariable Long id, @RequestBody QuestionUpsertRequest req) {
        Question q = adminService.saveQuestion(id, req, principal.getId());
        return interviewService.toQuestionDto(q);
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        adminService.deactivateQuestion(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/daily-question")
    public Map<String, Object> assignDailyQuestion(@AuthenticationPrincipal UserPrincipal principal,
                                                    @RequestBody Map<String, Long> body) {
        User admin = userRepository.findById(principal.getId()).orElseThrow();
        return dailyQuestionService.assign(admin, body.get("questionId"));
    }

    @GetMapping("/messages")
    public List<Map<String, Object>> messages() {
        return contactMessageService.listAll().stream().map(contactMessageService::toDto).toList();
    }

    @PatchMapping("/messages/{id}/read")
    public ResponseEntity<Void> markMessageRead(@PathVariable Long id) {
        contactMessageService.markRead(id);
        return ResponseEntity.ok().build();
    }

    private AdminUserDto toAdminUserDto(User u) {
        AdminUserDto dto = new AdminUserDto();
        dto.id = u.getId();
        dto.name = u.getName();
        dto.email = u.getEmail();
        dto.college = u.getCollege();
        dto.eloRating = Math.round(u.getEloRating() * 10) / 10.0;
        dto.skillLevel = u.getSkillLevel();
        dto.totalInterviews = u.getTotalInterviews();
        dto.avgScore = Math.round(u.getAvgScore() * 10) / 10.0;
        dto.createdAt = u.getCreatedAt();
        dto.isActiveAccount = u.getIsActiveAccount();
        return dto;
    }
}
