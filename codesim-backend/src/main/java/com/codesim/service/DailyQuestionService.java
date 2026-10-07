package com.codesim.service;

import com.codesim.dto.QuestionDto;
import com.codesim.entity.DailyQuestion;
import com.codesim.entity.Question;
import com.codesim.entity.User;
import com.codesim.repository.DailyQuestionRepository;
import com.codesim.repository.InterviewQuestionRepository;
import com.codesim.repository.QuestionRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.*;
import java.util.*;

@Service
public class DailyQuestionService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final DailyQuestionRepository dailyQuestionRepository;
    private final QuestionRepository questionRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final InterviewService interviewService;

    public DailyQuestionService(DailyQuestionRepository dailyQuestionRepository,
                                 QuestionRepository questionRepository,
                                 InterviewQuestionRepository interviewQuestionRepository,
                                 @Lazy InterviewService interviewService) {
        this.dailyQuestionRepository = dailyQuestionRepository;
        this.questionRepository = questionRepository;
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.interviewService = interviewService;
    }

    public static Instant nextExpiryIst(Instant from) {
        ZonedDateTime zdt = from.atZone(IST);
        ZonedDateTime todayFive = zdt.toLocalDate().atTime(5, 0).atZone(IST);
        if (!zdt.isBefore(todayFive)) {
            todayFive = todayFive.plusDays(1);
        }
        return todayFive.toInstant();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCurrentPublic() {
        return dailyQuestionRepository.findCurrent(Instant.now())
                .map(dq -> buildResponse(dq, null))
                .orElse(Map.of("available", false));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCurrentForUser(User user) {
        return dailyQuestionRepository.findCurrent(Instant.now())
                .map(dq -> buildResponse(dq, user))
                .orElse(Map.of("available", false));
    }

    private Map<String, Object> buildResponse(DailyQuestion dq, User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("available", true);
        m.put("expiresAt", dq.getExpiresAt());
        m.put("assignedAt", dq.getAssignedAt());
        QuestionDto dto = interviewService.toQuestionDto(dq.getQuestion());
        m.put("question", dto);
        if (user != null) {
            boolean solved = interviewQuestionRepository.findAllSubmittedByUser(user.getId()).stream()
                    .anyMatch(iq -> iq.getQuestion().getId().equals(dq.getQuestion().getId())
                            && "solved".equals(iq.getStatus()));
            m.put("solved", solved);
        }
        return m;
    }

    @Transactional
    public Map<String, Object> assign(User admin, Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        if (!Boolean.TRUE.equals(question.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question is inactive");
        }
        DailyQuestion dq = new DailyQuestion();
        dq.setQuestion(question);
        dq.setAssignedBy(admin);
        dq.setAssignedAt(Instant.now());
        dq.setExpiresAt(nextExpiryIst(Instant.now()));
        dailyQuestionRepository.save(dq);
        return buildResponse(dq, null);
    }

    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void expireDailyQuestions() {
        Instant now = Instant.now();
        dailyQuestionRepository.findAll().stream()
                .filter(dq -> !dq.getExpiresAt().isAfter(now))
                .forEach(dailyQuestionRepository::delete);
    }
}
