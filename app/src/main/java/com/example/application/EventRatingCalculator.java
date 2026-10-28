package com.example.application;

import java.util.List;
import java.util.Locale;

public class EventRatingCalculator {

    public static double calculateAverageScore(List<Rating> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        int count = 0;
        for (Rating rating : ratings) {
            if (rating != null && rating.getRating() > 0.0) {
                total += rating.getRating();
                count++;
            }
        }

        if (count == 0) {
            return 0.0;
        }

        double average = total / count;
        return Math.round(average * 10.0) / 10.0;
    }

    public static String formatAverageScore(double averageScore, int totalRatings) {
        if (totalRatings <= 0) {
            return "Nenhuma avaliação ainda";
        }
        return String.format(Locale.US, "Média: ⭐ %.1f / 5 (%d avaliações)", averageScore, totalRatings);
    }
}
