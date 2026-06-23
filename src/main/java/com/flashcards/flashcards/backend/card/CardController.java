package com.flashcards.flashcards.backend.card;

import com.flashcards.flashcards.backend.deck.Deck;
import com.flashcards.flashcards.backend.deck.DeckRepository;
import com.flashcards.flashcards.backend.security.AuthUser;
import com.flashcards.flashcards.backend.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api")
public class CardController {
    private final DeckRepository decks;
    private final FlashCardRepository cards;

    public CardController(DeckRepository decks, FlashCardRepository cards) {
        this.decks = decks;
        this.cards = cards;
    }

    @GetMapping("/decks/{deckId}/cards")
    public List<CardResponse> list(@AuthUser CurrentUser currentUser, @PathVariable UUID deckId) {
        Deck deck = findOwnedDeck(deckId, currentUser);
        return cards.findByDeckIdOrderByCreatedAtDesc(deck.getId()).stream()
                .map(CardResponse::from)
                .toList();
    }

    @PostMapping("/decks/{deckId}/cards")
    public CardResponse create(
            @AuthUser CurrentUser currentUser,
            @PathVariable UUID deckId,
            @Valid @RequestBody CardRequest request) {
        Deck deck = findOwnedDeck(deckId, currentUser);
        FlashCard card = new FlashCard();
        card.setDeck(deck);
        card.setQuestion(request.question().trim());
        card.setAnswer(request.answer().trim());
        card.setNextReviewDate(Instant.now());
        return CardResponse.from(cards.save(card));
    }

    @PutMapping("/cards/{id}")
    public CardResponse update(
            @AuthUser CurrentUser currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody CardRequest request) {
        FlashCard card = findOwnedCard(id, currentUser);
        card.setQuestion(request.question().trim());
        card.setAnswer(request.answer().trim());
        return CardResponse.from(cards.save(card));
    }

    @DeleteMapping("/cards/{id}")
    public void delete(@AuthUser CurrentUser currentUser, @PathVariable UUID id) {
        cards.delete(findOwnedCard(id, currentUser));
    }

    private Deck findOwnedDeck(UUID deckId, CurrentUser currentUser) {
        return decks.findByIdAndUserId(deckId, currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Paquet introuvable"));
    }

    private FlashCard findOwnedCard(UUID id, CurrentUser currentUser) {
        return cards.findByIdAndDeckUserId(id, currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Carte introuvable"));
    }

    public record CardRequest(@NotBlank String question, @NotBlank String answer) {
    }

    public record CardResponse(
            UUID id,
            String question,
            String answer,
            int boxLevel,
            Instant nextReviewDate,
            UUID deckId,
            Instant createdAt) {
        public static CardResponse from(FlashCard card) {
            return new CardResponse(
                    card.getId(),
                    card.getQuestion(),
                    card.getAnswer(),
                    card.getBoxLevel(),
                    card.getNextReviewDate(),
                    card.getDeck().getId(),
                    card.getCreatedAt());
        }
    }
}
