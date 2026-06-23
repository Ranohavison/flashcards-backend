package com.flashcards.flashcards.backend.review;

import com.flashcards.flashcards.backend.card.CardController.CardResponse;
import com.flashcards.flashcards.backend.card.FlashCard;
import com.flashcards.flashcards.backend.card.FlashCardRepository;
import com.flashcards.flashcards.backend.deck.DeckRepository;
import com.flashcards.flashcards.backend.security.AuthUser;
import com.flashcards.flashcards.backend.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/review")
public class ReviewController {
    private static final int MAX_BOX = 5;
    private static final int[] INTERVAL_DAYS = {0, 1, 3, 7, 14, 30};

    private final FlashCardRepository cards;
    private final DeckRepository decks;
    private final ReviewLogRepository reviewLogs;

    public ReviewController(FlashCardRepository cards, DeckRepository decks, ReviewLogRepository reviewLogs) {
        this.cards = cards;
        this.decks = decks;
        this.reviewLogs = reviewLogs;
    }

    @GetMapping("/due")
    public List<CardResponse> due(@AuthUser CurrentUser currentUser, @RequestParam(required = false) UUID deckId) {
        Instant now = Instant.now();
        List<FlashCard> dueCards = deckId == null
                ? cards.findDueCards(currentUser.id(), now)
                : cards.findDueCardsForDeck(currentUser.id(), deckId, now);
        return dueCards.stream().map(CardResponse::from).toList();
    }

    @GetMapping("/sprint/{deckId}")
    public List<CardResponse> sprint(@AuthUser CurrentUser currentUser, @PathVariable UUID deckId) {
        if (decks.findByIdAndUserId(deckId, currentUser.id()).isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "Paquet introuvable");
        }
        return cards.findByDeckIdOrderByCreatedAtDesc(deckId).stream().map(CardResponse::from).toList();
    }

    @PostMapping("/{cardId}")
    public CardResponse review(
            @AuthUser CurrentUser currentUser,
            @PathVariable UUID cardId,
            @Valid @RequestBody ReviewRequest request) {
        FlashCard card = cards.findByIdAndDeckUserId(cardId, currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Carte introuvable"));

        ReviewLog log = new ReviewLog();
        log.setCard(card);
        log.setReviewedAt(Instant.now());
        log.setStatus(request.status());
        reviewLogs.save(log);

        if (!request.sprint()) {
            if (request.status() == ReviewStatus.FAILED) {
                card.setBoxLevel(1);
            } else {
                card.setBoxLevel(Math.min(MAX_BOX, card.getBoxLevel() + 1));
            }
            card.setNextReviewDate(Instant.now().plus(INTERVAL_DAYS[card.getBoxLevel()], ChronoUnit.DAYS));
            cards.save(card);
        }
        return CardResponse.from(card);
    }

    public record ReviewRequest(@NotNull ReviewStatus status, boolean sprint) {
    }
}
