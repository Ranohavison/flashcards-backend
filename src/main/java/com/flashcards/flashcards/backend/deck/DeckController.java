package com.flashcards.flashcards.backend.deck;

import com.flashcards.flashcards.backend.security.AuthUser;
import com.flashcards.flashcards.backend.security.CurrentUser;
import com.flashcards.flashcards.backend.user.AppUser;
import com.flashcards.flashcards.backend.user.AppUserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/decks")
public class DeckController {
    private final DeckRepository decks;
    private final AppUserRepository users;

    public DeckController(DeckRepository decks, AppUserRepository users) {
        this.decks = decks;
        this.users = users;
    }

    @GetMapping
    public List<DeckResponse> list(@AuthUser CurrentUser currentUser) {
        return decks.findByUserIdOrderByCreatedAtDesc(currentUser.id()).stream()
                .map(DeckResponse::from)
                .toList();
    }

    @PostMapping
    public DeckResponse create(@AuthUser CurrentUser currentUser, @Valid @RequestBody DeckRequest request) {
        AppUser user = users.getReferenceById(currentUser.id());
        Deck deck = new Deck();
        deck.setName(request.name().trim());
        deck.setUser(user);
        return DeckResponse.from(decks.save(deck));
    }

    @PutMapping("/{id}")
    public DeckResponse update(
            @AuthUser CurrentUser currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody DeckRequest request) {
        Deck deck = findOwned(id, currentUser);
        deck.setName(request.name().trim());
        return DeckResponse.from(decks.save(deck));
    }

    @PatchMapping("/{id}/archive")
    public DeckResponse archive(@AuthUser CurrentUser currentUser, @PathVariable UUID id) {
        Deck deck = findOwned(id, currentUser);
        deck.setArchived(!deck.isArchived());
        return DeckResponse.from(decks.save(deck));
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthUser CurrentUser currentUser, @PathVariable UUID id) {
        decks.delete(findOwned(id, currentUser));
    }

    private Deck findOwned(UUID id, CurrentUser currentUser) {
        return decks.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Paquet introuvable"));
    }

    public record DeckRequest(@NotBlank @Size(max = 100) String name) {
    }

    public record DeckResponse(UUID id, String name, boolean archived, Instant createdAt) {
        static DeckResponse from(Deck deck) {
            return new DeckResponse(deck.getId(), deck.getName(), deck.isArchived(), deck.getCreatedAt());
        }
    }
}
