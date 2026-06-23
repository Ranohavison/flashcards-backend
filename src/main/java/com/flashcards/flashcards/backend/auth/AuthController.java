package com.flashcards.flashcards.backend.auth;

import com.flashcards.flashcards.backend.security.AuthUser;
import com.flashcards.flashcards.backend.security.CurrentUser;
import com.flashcards.flashcards.backend.security.JwtService;
import com.flashcards.flashcards.backend.user.AppUser;
import com.flashcards.flashcards.backend.user.AppUserRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final boolean secureCookie;

    public AuthController(
            AppUserRepository users,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${app.cookie.secure:false}") boolean secureCookie) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request, HttpServletResponse response) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(BAD_REQUEST, "Email deja utilise");
        }
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        users.save(user);
        return authenticated(user, response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request, HttpServletResponse response) {
        AppUser user = users.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Identifiants invalides"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Identifiants invalides");
        }
        return authenticated(user, response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserResponse me(@AuthUser CurrentUser user) {
        return new UserResponse(user.id(), user.email());
    }

    private ResponseEntity<AuthResponse> authenticated(AppUser user, HttpServletResponse response) {
        String token = jwtService.createToken(user.getId(), user.getEmail());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(token, Duration.ofDays(7)).toString());
        return ResponseEntity.ok(new AuthResponse(token, new UserResponse(user.getId(), user.getEmail())));
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from("access_token", value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    public record AuthRequest(@Email @NotBlank String email, @Size(min = 8) String password) {
    }

    public record AuthResponse(String token, UserResponse user) {
    }

    public record UserResponse(UUID id, String email) {
    }
}
