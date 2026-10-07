package com.codesim.service;

import com.codesim.dto.AuthDtos.*;
import com.codesim.entity.User;
import com.codesim.repository.UserRepository;
import com.codesim.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email.toLowerCase())) {
            throw new IllegalArgumentException("Email already registered.");
        }
        User user = new User();
        user.setName(req.name.trim());
        user.setEmail(req.email.trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(req.password));
        user.setCollege(req.college == null ? "" : req.college.trim());
        user.setRole("USER");
        userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token, toDto(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email.trim().toLowerCase(), req.password));
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        User user = userRepository.findByEmail(req.email.trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        if (!user.getIsActiveAccount()) {
            throw new BadCredentialsException("Account is suspended.");
        }

        user.setLastActive(Instant.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token, toDto(user));
    }

    public UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.id = user.getId();
        dto.name = user.getName();
        dto.email = user.getEmail();
        dto.role = user.getRole();
        dto.eloRating = Math.round(user.getEloRating() * 10) / 10.0;
        dto.skillLevel = user.getSkillLevel();
        dto.totalInterviews = user.getTotalInterviews();
        dto.totalQuestionsSolved = user.getTotalQuestionsSolved();
        dto.avgScore = Math.round(user.getAvgScore() * 10) / 10.0;
        dto.college = user.getCollege();
        dto.bio = user.getBio();
        dto.preferredLanguage = user.getPreferredLanguage();
        dto.themePreference = user.getThemePreference();
        dto.hasCompletedAssessment = user.getHasCompletedAssessment();
        return dto;
    }
}
