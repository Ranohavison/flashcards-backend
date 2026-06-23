package com.flashcards.flashcards.backend.security;

import java.util.UUID;

public record CurrentUser(UUID id, String email) {
}
