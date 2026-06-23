package com.flashcards.flashcards.backend.stats;

import com.flashcards.flashcards.backend.card.FlashCardRepository;
import com.flashcards.flashcards.backend.review.ReviewLogRepository;
import com.flashcards.flashcards.backend.security.AuthUser;
import com.flashcards.flashcards.backend.security.CurrentUser;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {
    private final FlashCardRepository cards;
    private final ReviewLogRepository reviewLogs;

    public StatsController(FlashCardRepository cards, ReviewLogRepository reviewLogs) {
        this.cards = cards;
        this.reviewLogs = reviewLogs;
    }

    @GetMapping
    public StatsResponse stats(@AuthUser CurrentUser currentUser) {
        List<BoxCount> boxes = cards.countByBoxLevel(currentUser.id()).stream()
                .map(row -> new BoxCount((Integer) row[0], (Long) row[1]))
                .toList();

        Instant since = Instant.now().minus(29, ChronoUnit.DAYS);
        Map<LocalDate, Long> successes = reviewLogs.successfulReviewsByDay(currentUser.id(), since).stream()
                .collect(Collectors.toMap(row -> ((Date) row[0]).toLocalDate(), row -> ((Number) row[1]).longValue()));

        List<ProgressPoint> progress = new ArrayList<>();
        long total = 0;
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(29);
        for (int i = 0; i < 30; i++) {
            LocalDate day = start.plusDays(i);
            total += successes.getOrDefault(day, 0L);
            progress.add(new ProgressPoint(day, total));
        }
        return new StatsResponse(boxes, progress);
    }

    public record StatsResponse(List<BoxCount> boxes, List<ProgressPoint> progress) {
    }

    public record BoxCount(int boxLevel, long count) {
    }

    public record ProgressPoint(LocalDate date, long learned) {
    }
}
