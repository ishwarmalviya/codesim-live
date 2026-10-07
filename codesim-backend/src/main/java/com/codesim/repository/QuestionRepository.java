package com.codesim.repository;

import com.codesim.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    long countByIsActiveTrue();

    @Query("SELECT DISTINCT q.topic FROM Question q ORDER BY q.topic")
    List<String> findDistinctTopics();

    @Query("SELECT q FROM Question q WHERE q.isActive = true AND q.id NOT IN :excludedIds " +
           "AND q.eloRating BETWEEN :minElo AND :maxElo " +
           "AND (:topic IS NULL OR q.topic = :topic)")
    List<Question> findCandidates(List<Long> excludedIds, Double minElo, Double maxElo, String topic);

    @Query("SELECT q FROM Question q WHERE q.isActive = true AND q.id NOT IN :excludedIds " +
           "AND (:topic IS NULL OR q.topic = :topic) ORDER BY ABS(q.eloRating - :targetElo) ASC")
    List<Question> findBroadCandidates(List<Long> excludedIds, String topic, Double targetElo, Pageable pageable);

    Page<Question> findAll(Pageable pageable);

    @Query("SELECT q FROM Question q WHERE " +
           "(:searchPattern IS NULL OR LOWER(q.title) LIKE :searchPattern) AND " +
           "(:difficulty IS NULL OR q.difficulty = :difficulty) AND " +
           "(:topic IS NULL OR q.topic = :topic) ORDER BY q.createdAt DESC")
    Page<Question> search(String searchPattern, String difficulty, String topic, Pageable pageable);

    @Query("SELECT q.topic, COUNT(q) FROM Question q GROUP BY q.topic")
    List<Object[]> countByTopic();

    @Query("SELECT q.difficulty, COUNT(q) FROM Question q GROUP BY q.difficulty")
    List<Object[]> countByDifficulty();

    @Query("SELECT q.topic, COUNT(q), AVG(q.eloRating), COALESCE(SUM(q.timesAttempted), 0), COALESCE(SUM(q.timesSolved), 0) " +
           "FROM Question q GROUP BY q.topic")
    List<Object[]> topicAnalytics();
}
