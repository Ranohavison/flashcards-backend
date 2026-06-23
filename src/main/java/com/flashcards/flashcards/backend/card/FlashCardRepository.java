package com.flashcards.flashcards.backend.card;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FlashCardRepository extends JpaRepository<FlashCard, UUID> {
    List<FlashCard> findByDeckIdOrderByCreatedAtDesc(UUID deckId);

    Optional<FlashCard> findByIdAndDeckUserId(UUID id, UUID userId);

    @Query("""
        select c from FlashCard c
        where c.deck.user.id = :userId
        and c.deck.archived = false
        and c.nextReviewDate <= :now
        order by c.nextReviewDate asc
    """)
    List<FlashCard> findDueCards(UUID userId, Instant now);

    @Query("""
        select c from FlashCard c
        where c.deck.id = :deckId
        and c.deck.user.id = :userId
        and c.deck.archived = false
        and c.nextReviewDate <= :now
        order by c.nextReviewDate asc
    """)
    List<FlashCard> findDueCardsForDeck(UUID userId, UUID deckId, Instant now);

    @Query("""
        select c.boxLevel, count(c) from FlashCard c
        where c.deck.user.id = :userId
        group by c.boxLevel
        order by c.boxLevel
    """)
    List<Object[]> countByBoxLevel(UUID userId);
}
