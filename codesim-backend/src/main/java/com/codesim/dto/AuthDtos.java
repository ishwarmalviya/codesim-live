package com.codesim.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public static class RegisterRequest {
        @NotBlank
        public String name;
        @NotBlank @Email
        public String email;
        @NotBlank @Size(min = 6)
        public String password;
        public String college;
    }

    public static class LoginRequest {
        @NotBlank @Email
        public String email;
        @NotBlank
        public String password;
    }

    public static class AuthResponse {
        public String token;
        public UserDto user;

        public AuthResponse(String token, UserDto user) {
            this.token = token;
            this.user = user;
        }
    }

    public static class UserDto {
        public Long id;
        public String name;
        public String email;
        public String role;
        public Double eloRating;
        public String skillLevel;
        public Integer totalInterviews;
        public Integer totalQuestionsSolved;
        public Double avgScore;
        public String college;
        public String bio;
        public String preferredLanguage;
        public String themePreference;
        public Boolean hasCompletedAssessment;
    }
}
