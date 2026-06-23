package com.flashcards.flashcards.backend.deck;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckRepository extends JpaRepository<Deck, UUID> {
    List<Deck> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Deck> findByIdAndUserId(UUID id, UUID userId);
}
