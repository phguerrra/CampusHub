package com.example.application;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EventRatingCalculatorTest {

    @Test
    public void calculateAverageScore_nullOrEmptyList_returnsZero() {
        assertEquals(0.0, EventRatingCalculator.calculateAverageScore(null), 0.001);
        assertEquals(0.0, EventRatingCalculator.calculateAverageScore(Collections.emptyList()), 0.001);
    }

    @Test
    public void calculateAverageScore_singleAndMultipleRatings() {
        List<Rating> singleList = new ArrayList<>();
        singleList.add(new Rating("1", "event1", "user1", 5.0f, null, null));
        assertEquals(5.0, EventRatingCalculator.calculateAverageScore(singleList), 0.001);

        List<Rating> multipleList = new ArrayList<>();
        multipleList.add(new Rating("1", "event1", "user1", 5.0f, null, null));
        multipleList.add(new Rating("2", "event1", "user2", 4.0f, null, null));
        multipleList.add(new Rating("3", "event1", "user3", 3.0f, null, null));
        assertEquals(4.0, EventRatingCalculator.calculateAverageScore(multipleList), 0.001);
    }

    @Test
    public void calculateAverageScore_roundingToOneDecimalPlace() {
        List<Rating> ratings = new ArrayList<>();
        ratings.add(new Rating("1", "event1", "user1", 5.0f, null, null));
        ratings.add(new Rating("2", "event1", "user2", 4.0f, null, null));
        ratings.add(new Rating("3", "event1", "user3", 4.0f, null, null)); // Média 13/3 = 4.333333
        assertEquals(4.3, EventRatingCalculator.calculateAverageScore(ratings), 0.001);
    }

    @Test
    public void formatAverageScore_emptyAndFilledCases() {
        assertEquals("Nenhuma avaliação ainda", EventRatingCalculator.formatAverageScore(0.0, 0));
        String formatted = EventRatingCalculator.formatAverageScore(4.5, 12);
        assertEquals("Média: ⭐ 4.5 / 5 (12 avaliações)", formatted);
    }
}
