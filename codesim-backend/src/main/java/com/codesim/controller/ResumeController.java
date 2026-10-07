package com.codesim.controller;

import com.codesim.dto.PhaseTwoDtos.*;
import com.codesim.entity.Resume;
import com.codesim.entity.User;
import com.codesim.repository.UserRepository;
import com.codesim.security.UserPrincipal;
import com.codesim.service.ResumeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private final ResumeService resumeService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ResumeController(ResumeService resumeService, UserRepository userRepository, ObjectMapper objectMapper) {
        this.resumeService = resumeService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    private User currentUser(UserPrincipal principal) {
        return userRepository.findById(principal.getId()).orElseThrow();
    }

    @GetMapping
    public ResponseEntity<ResumeDto> get(@AuthenticationPrincipal UserPrincipal principal) {
        return resumeService.getResume(currentUser(principal).getId())
                .map(r -> ResponseEntity.ok(toDto(r)))
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResumeDto upload(@AuthenticationPrincipal UserPrincipal principal, @RequestBody ResumeUploadRequest req) {
        if (req.rawText == null || req.rawText.isBlank()) {
            throw new IllegalArgumentException("Resume text is required.");
        }
        Resume resume = resumeService.analyzeAndSave(currentUser(principal), req.rawText);
        return toDto(resume);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResumeDto uploadPdf(@AuthenticationPrincipal UserPrincipal principal,
                                @RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("PDF file is required.");
        }
        if (!"application/pdf".equals(file.getContentType()) && !file.getOriginalFilename().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are supported.");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File must be under 5MB.");
        }
        String text = resumeService.extractTextFromPdf(file.getBytes());
        if (text.isBlank()) {
            throw new IllegalArgumentException("Could not extract text from PDF.");
        }
        Resume resume = resumeService.analyzeAndSave(currentUser(principal), text);
        return toDto(resume);
    }

    private ResumeDto toDto(Resume r) {
        ResumeDto dto = new ResumeDto();
        dto.rawText = r.getRawText();
        dto.experienceLevel = r.getExperienceLevel();
        dto.uploadedAt = r.getUploadedAt();
        try {
            dto.parsedSkills = objectMapper.readerForListOf(String.class).readValue(r.getParsedSkills());
        } catch (Exception e) {
            dto.parsedSkills = List.of();
        }
        dto.atsScore = r.getAtsScore();
        try {
            dto.analysis = r.getAnalysisJson() != null && !r.getAnalysisJson().isBlank()
                    ? objectMapper.readTree(r.getAnalysisJson()) : null;
        } catch (Exception e) {
            dto.analysis = null;
        }
        return dto;
    }
}
