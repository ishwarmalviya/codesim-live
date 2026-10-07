package com.codesim.controller;

import com.codesim.dto.DashboardDtos.*;
import com.codesim.entity.User;
import com.codesim.repository.UserRepository;
import com.codesim.security.UserPrincipal;
import com.codesim.service.DashboardService;
import com.codesim.service.DailyQuestionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DailyQuestionService dailyQuestionService;

    public DashboardController(DashboardService dashboardService, UserRepository userRepository,
                                PasswordEncoder passwordEncoder, DailyQuestionService dailyQuestionService) {
        this.dashboardService = dashboardService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.dailyQuestionService = dailyQuestionService;
    }

    private User currentUser(UserPrincipal principal) {
        return userRepository.findById(principal.getId()).orElseThrow();
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.getDashboard(currentUser(principal));
    }

    @GetMapping("/profile")
    public ProfileResponse profile(@AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.getProfile(currentUser(principal));
    }

    @PutMapping("/profile")
    public ProfileResponse updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                          @RequestBody UpdateProfileRequest req) {
        User updated = dashboardService.updateProfile(currentUser(principal), req, passwordEncoder);
        return dashboardService.getProfile(updated);
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardEntry> leaderboard() {
        return dashboardService.getLeaderboard();
    }

    @GetMapping("/daily-question")
    public java.util.Map<String, Object> dailyQuestion(@AuthenticationPrincipal UserPrincipal principal) {
        return dailyQuestionService.getCurrentForUser(currentUser(principal));
    }
}
