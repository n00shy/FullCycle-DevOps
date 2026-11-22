package com.fullcycle.notes.auth;

import com.fullcycle.notes.user.AppUserDetails;
import com.fullcycle.notes.user.AppUser;
import com.fullcycle.notes.user.Role;
import com.fullcycle.notes.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.*;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager manager;
    private final SecurityContextRepository contexts;
    private final SessionAuthenticationStrategy sessionStrategy;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            AuthenticationManager manager,
            SecurityContextRepository contexts,
            SessionAuthenticationStrategy sessionStrategy,
            UserRepository users,
            PasswordEncoder passwordEncoder) {
        this.manager = manager;
        this.contexts = contexts;
        this.sessionStrategy = sessionStrategy;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        // Calling getToken() materializes the deferred token and sets XSRF-TOKEN.
        token.getToken();
        return Map.of("headerName", token.getHeaderName());
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest body) {
        String username = body.username().trim();
        String email = body.email().trim().toLowerCase(java.util.Locale.ROOT);

        if (body.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must not exceed 72 UTF-8 bytes");
        }
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        AppUser created;
        try {
            created = users.saveAndFlush(new AppUser(
                    username, email, passwordEncoder.encode(body.password()), Role.USER));
        } catch (DataIntegrityViolationException ex) {
            // Database unique indexes remain the final guard against concurrent signups.
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Username or email already exists");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(new UserResponse(
                created.getId(), created.getUsername(), created.getEmail(), created.getRole().name()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest body,
            HttpServletRequest req,
            HttpServletResponse res) {
        try {
            Authentication auth = manager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));
            sessionStrategy.onAuthentication(auth, req, res);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            contexts.saveContext(context, req, res);
            AppUserDetails user = (AppUserDetails) auth.getPrincipal();
            return ResponseEntity.ok(new UserResponse(
                    user.id(), user.username(), user.email(), user.role().name()));
        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username/email or password"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req, HttpServletResponse res) {
        SecurityContextHolder.clearContext();
        contexts.saveContext(SecurityContextHolder.createEmptyContext(), req, res);
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserResponse me(Authentication auth) {
        AppUserDetails user = (AppUserDetails) auth.getPrincipal();
        return new UserResponse(user.id(), user.username(), user.email(), user.role().name());
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50)
            @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                    message = "Username may contain letters, numbers, dots, underscores and hyphens only")
            String username,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 12, max = 72) String password) {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record UserResponse(Long id, String username, String email, String role) {}
}
