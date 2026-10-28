package com.example.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EventFilterHelper {

    public static List<Event> filterEvents(List<Event> events, String searchQuery, String categoryFilter,
                                           String statusFilter, long nowTimestamp, String upcomingLabel, String endedLabel) {
        List<Event> filtered = new ArrayList<>();
        if (events == null || events.isEmpty()) {
            return filtered;
        }

        String query = searchQuery != null ? searchQuery.trim().toLowerCase(Locale.getDefault()) : "";
        String category = categoryFilter != null ? categoryFilter.trim() : "";
        String status = statusFilter != null ? statusFilter.trim() : "";

        for (Event event : events) {
            if (event == null) {
                continue;
            }

            // Search by title (case-insensitive substring)
            if (!query.isEmpty()) {
                String name = event.getName() != null ? event.getName().toLowerCase(Locale.getDefault()) : "";
                if (!name.contains(query)) {
                    continue;
                }
            }

            // Category filter
            if (!category.isEmpty()) {
                String cat = event.getCategory() != null ? event.getCategory().trim() : "";
                if (!cat.equalsIgnoreCase(category)) {
                    continue;
                }
            }

            // Status filter (Próximos vs Encerrados)
            if (!status.isEmpty()) {
                boolean isEnded = event.getDate() != null && event.getDate().toDate().getTime() < nowTimestamp;
                if (status.equalsIgnoreCase(upcomingLabel) && isEnded) {
                    continue;
                }
                if (status.equalsIgnoreCase(endedLabel) && !isEnded) {
                    continue;
                }
            }

            filtered.add(event);
        }

        return filtered;
    }
}
