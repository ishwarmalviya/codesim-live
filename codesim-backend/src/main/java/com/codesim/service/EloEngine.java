package com.codesim.service;

public class EloEngine {

    public static final double K_USER = 32;
    public static final double K_QUESTION = 16;

    public static double expectedScore(double userElo, double questionElo) {
        return 1 / (1 + Math.pow(10, (questionElo - userElo) / 400));
    }

    public static double updateUserElo(double userElo, double expected, double actual) {
        double newElo = userElo + K_USER * (actual - expected);
        return Math.max(400, Math.min(2500, newElo));
    }

    public static double updateQuestionElo(double questionElo, double expected, double actual) {
        double newElo = questionElo + K_QUESTION * (expected - actual);
        return Math.max(400, Math.min(2500, newElo));
    }

    public static String getDifficultyLabel(double elo) {
        if (elo < 1100) return "easy";
        if (elo < 1400) return "medium";
        if (elo < 1700) return "hard";
        return "expert";
    }
}
