package com.codesim.controller;

import com.codesim.dto.PhaseThreeDtos.ReplayEventDto;
import com.codesim.entity.InterviewQuestion;
import com.codesim.entity.User;
import com.codesim.repository.InterviewQuestionRepository;
import com.codesim.repository.UserRepository;
import com.codesim.security.UserPrincipal;
import com.codesim.service.ReplayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/interviews/questions")
public class ReplayController {

    private final ReplayService replayService;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ReplayController(ReplayService replayService, InterviewQuestionRepository interviewQuestionRepository,
                             UserRepository userRepository, ObjectMapper objectMapper) {
        this.replayService = replayService;
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/{iqId}/replay")
    @Transactional(readOnly = true)
    public List<ReplayEventDto> replay(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long iqId) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        InterviewQuestion iq = interviewQuestionRepository.findById(iqId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        if (!iq.getInterview().getUser().getId().equals(user.getId()) && !user.getRole().equals("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your interview");
        }

        return replayService.getTimeline(iqId).stream().map(e -> {
            ReplayEventDto dto = new ReplayEventDto();
            dto.id = e.getId();
            dto.eventType = e.getEventType();
            dto.codeSnapshot = e.getCodeSnapshot();
            dto.createdAt = e.getCreatedAt();
            try {
                dto.payload = objectMapper.readTree(e.getPayload());
            } catch (Exception ex) {
                dto.payload = objectMapper.createObjectNode();
            }
            return dto;
        }).toList();
    }
}
