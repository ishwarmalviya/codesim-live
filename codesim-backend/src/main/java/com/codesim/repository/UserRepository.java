package com.codesim.repository;

import com.codesim.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(String role);
    long countByRoleAndLastActiveGreaterThanEqual(String role, Instant since);
    List<User> findByRoleOrderByCreatedAtDesc(String role);
    Page<User> findByRoleAndNameContainingIgnoreCaseOrRoleAndEmailContainingIgnoreCase(
            String role1, String name, String role2, String email, Pageable pageable);
    Page<User> findByRole(String role, Pageable pageable);
    List<User> findByRoleAndIsActiveAccountTrueOrderByEloRatingDesc(String role, Pageable pageable);
    long countByRoleAndEloRatingGreaterThan(String role, Double eloRating);
    long countByRoleAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(String role, Instant from, Instant to);
}
