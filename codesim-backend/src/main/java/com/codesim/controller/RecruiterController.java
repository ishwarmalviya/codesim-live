package com.codesim.controller;

import com.codesim.dto.PhaseThreeDtos.*;
import com.codesim.entity.PlacementScore;
import com.codesim.entity.RecruiterShortlist;
import com.codesim.entity.User;
import com.codesim.repository.UserRepository;
import com.codesim.security.UserPrincipal;
import com.codesim.service.RecruiterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recruiter")
public class RecruiterController {

    private final RecruiterService recruiterService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public RecruiterController(RecruiterService recruiterService, UserRepository userRepository,
                                ObjectMapper objectMapper) {
        this.recruiterService = recruiterService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    private User currentUser(UserPrincipal principal) {
        return userRepository.findById(principal.getId()).orElseThrow();
    }

    @GetMapping("/candidates")
    @Transactional(readOnly = true)
    public Page<CandidateDto> candidates(@AuthenticationPrincipal UserPrincipal principal,
                                          @RequestParam(required = false) Double minScore,
                                          @RequestParam(defaultValue = "1") int page) {
        User recruiter = currentUser(principal);
        Map<Long, RecruiterShortlist> shortlisted = recruiterService.getShortlist(recruiter.getId()).stream()
                .collect(Collectors.toMap(s -> s.getCandidate().getId(), s -> s));

        return recruiterService.searchCandidates(minScore, page).map(score -> {
            User candidate = score.getUser();
            CandidateDto dto = new CandidateDto();
            dto.userId = candidate.getId();
            dto.name = candidate.getName();
            dto.email = candidate.getEmail();
            dto.college = candidate.getCollege();
            dto.eloRating = Math.round(candidate.getEloRating() * 10) / 10.0;
            dto.skillLevel = candidate.getSkillLevel();
            dto.placementScore = score.getOverallScore();
            try {
                dto.breakdown = objectMapper.readTree(score.getBreakdown());
            } catch (Exception e) {
                dto.breakdown = objectMapper.createObjectNode();
            }
            RecruiterShortlist entry = shortlisted.get(candidate.getId());
            dto.shortlisted = entry != null;
            dto.note = entry != null ? entry.getNote() : "";
            return dto;
        });
    }

    @PostMapping("/candidates/{userId}/shortlist")
    public ResponseEntity<Void> shortlist(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long userId,
                                           @RequestBody(required = false) ShortlistRequest req) {
        recruiterService.shortlist(currentUser(principal), userId, req != null ? req.note : "");
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/candidates/{userId}/shortlist")
    public ResponseEntity<Void> removeShortlist(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long userId) {
        recruiterService.removeShortlist(currentUser(principal), userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/shortlist")
    @Transactional(readOnly = true)
    public List<CandidateDto> shortlist(@AuthenticationPrincipal UserPrincipal principal) {
        User recruiter = currentUser(principal);
        return recruiterService.getShortlist(recruiter.getId()).stream().map(entry -> {
            User candidate = entry.getCandidate();
            CandidateDto dto = new CandidateDto();
            dto.userId = candidate.getId();
            dto.name = candidate.getName();
            dto.email = candidate.getEmail();
            dto.college = candidate.getCollege();
            dto.eloRating = Math.round(candidate.getEloRating() * 10) / 10.0;
            dto.skillLevel = candidate.getSkillLevel();
            dto.shortlisted = true;
            dto.note = entry.getNote();
            return dto;
        }).toList();
    }
}
