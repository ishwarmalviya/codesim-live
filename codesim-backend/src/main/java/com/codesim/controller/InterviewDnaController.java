package com.codesim.controller;

import com.codesim.dto.PhaseTwoDtos.InterviewDnaDto;
import com.codesim.entity.User;
import com.codesim.repository.UserRepository;
import com.codesim.security.UserPrincipal;
import com.codesim.service.InterviewDnaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interview-dna")
public class InterviewDnaController {

    private final InterviewDnaService interviewDnaService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public InterviewDnaController(InterviewDnaService interviewDnaService, UserRepository userRepository,
                                   ObjectMapper objectMapper) {
        this.interviewDnaService = interviewDnaService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    private User currentUser(UserPrincipal principal) {
        return userRepository.findById(principal.getId()).orElseThrow();
    }

    @GetMapping
    public InterviewDnaDto get(@AuthenticationPrincipal UserPrincipal principal) {
        var profile = interviewDnaService.getOrCompute(currentUser(principal));
        return toDto(profile);
    }

    @PostMapping("/recompute")
    public InterviewDnaDto recompute(@AuthenticationPrincipal UserPrincipal principal) {
        var profile = interviewDnaService.recompute(currentUser(principal));
        return toDto(profile);
    }

    private InterviewDnaDto toDto(com.codesim.entity.InterviewDnaProfile profile) {
        InterviewDnaDto dto = new InterviewDnaDto();
        try {
            dto.traits = objectMapper.readTree(profile.getTraits());
        } catch (Exception e) {
            dto.traits = objectMapper.createObjectNode();
        }
        dto.narrativeText = profile.getNarrativeText();
        dto.computedAt = profile.getComputedAt();
        return dto;
    }
}
