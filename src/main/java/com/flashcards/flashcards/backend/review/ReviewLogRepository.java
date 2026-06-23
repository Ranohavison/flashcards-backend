package com.flashcards.flashcards.backend.review;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewLogRepository extends JpaRepository<ReviewLog, UUID> {
    @Query(value = """
        select cast(r.reviewed_at as date) as review_day, count(*) as total
        from review_log r
        join card c on c.id = r.card_id
        join deck d on d.id = c.deck_id
        where d.user_id = :userId
        and r.status = 'SUCCESS'
        and r.reviewed_at >= :since
        group by review_day
        order by review_day
    """, nativeQuery = true)
    List<Object[]> successfulReviewsByDay(UUID userId, Instant since);
}
