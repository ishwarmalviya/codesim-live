package com.codesim.service;

import com.codesim.entity.PlacementScore;
import com.codesim.entity.RecruiterShortlist;
import com.codesim.entity.User;
import com.codesim.repository.PlacementScoreRepository;
import com.codesim.repository.RecruiterShortlistRepository;
import com.codesim.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RecruiterService {

    private final PlacementScoreRepository placementScoreRepository;
    private final RecruiterShortlistRepository shortlistRepository;
    private final UserRepository userRepository;

    public RecruiterService(PlacementScoreRepository placementScoreRepository,
                             RecruiterShortlistRepository shortlistRepository,
                             UserRepository userRepository) {
        this.placementScoreRepository = placementScoreRepository;
        this.shortlistRepository = shortlistRepository;
        this.userRepository = userRepository;
    }

    public Page<PlacementScore> searchCandidates(Double minScore, int page) {
        return placementScoreRepository.searchCandidates(minScore, PageRequest.of(Math.max(0, page - 1), 20));
    }

    @Transactional
    public void shortlist(User recruiter, Long candidateId, String note) {
        RecruiterShortlist entry = shortlistRepository.findByRecruiterIdAndCandidateId(recruiter.getId(), candidateId)
                .orElseGet(() -> {
                    RecruiterShortlist s = new RecruiterShortlist();
                    s.setRecruiter(recruiter);
                    s.setCandidate(userRepository.findById(candidateId).orElseThrow());
                    return s;
                });
        entry.setNote(note == null ? "" : note);
        shortlistRepository.save(entry);
    }

    @Transactional
    public void removeShortlist(User recruiter, Long candidateId) {
        shortlistRepository.deleteByRecruiterIdAndCandidateId(recruiter.getId(), candidateId);
    }

    public List<RecruiterShortlist> getShortlist(Long recruiterId) {
        return shortlistRepository.findByRecruiterIdOrderByAddedAtDesc(recruiterId);
    }
}
