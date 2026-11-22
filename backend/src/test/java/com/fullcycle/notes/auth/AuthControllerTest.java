package com.fullcycle.notes.auth;

import com.fullcycle.notes.user.AppUser;
import com.fullcycle.notes.user.Role;
import com.fullcycle.notes.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthControllerTest {
    private UserRepository users;
    private PasswordEncoder passwordEncoder;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        controller = new AuthController(
                mock(AuthenticationManager.class),
                mock(SecurityContextRepository.class),
                mock(SessionAuthenticationStrategy.class),
                users,
                passwordEncoder);
    }

    @Test
    void registerCreatesStandardUserAndStoresOnlyEncodedPassword() {
        String rawPassword = "correct-horse-battery";
        AppUser saved = mock(AppUser.class);
        when(users.existsByUsernameIgnoreCase("new.user")).thenReturn(false);
        when(users.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn("$2a$encoded");
        when(users.saveAndFlush(any(AppUser.class))).thenReturn(saved);
        when(saved.getId()).thenReturn(42L);
        when(saved.getUsername()).thenReturn("new.user");
        when(saved.getEmail()).thenReturn("new@example.com");
        when(saved.getRole()).thenReturn(Role.USER);

        var response = controller.register(new AuthController.RegisterRequest(
                "new.user", "NEW@example.com", rawPassword));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(42L, response.getBody().id());
        assertEquals("USER", response.getBody().role());

        var captor = org.mockito.ArgumentCaptor.forClass(AppUser.class);
        verify(users).saveAndFlush(captor.capture());
        assertEquals("new.user", captor.getValue().getUsername());
        assertEquals("new@example.com", captor.getValue().getEmail());
        assertEquals("$2a$encoded", captor.getValue().getPasswordHash());
        assertEquals(Role.USER, captor.getValue().getRole());
        verify(passwordEncoder).encode(rawPassword);
    }

    @Test
    void registerRejectsExistingUsername() {
        when(users.existsByUsernameIgnoreCase("taken")).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.register(new AuthController.RegisterRequest(
                        "taken", "new@example.com", "correct-horse-battery")));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(users, never()).saveAndFlush(any(AppUser.class));
    }

    @Test
    void registerRejectsPasswordLongerThanBcryptByteLimit() {
        String password = "é".repeat(37); // 37 characters, 74 UTF-8 bytes.

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.register(new AuthController.RegisterRequest(
                        "new.user", "new@example.com", password)));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(passwordEncoder, never()).encode(any());
        verify(users, never()).saveAndFlush(any(AppUser.class));
    }

    @Test
    void registerConvertsConcurrentUniqueConstraintViolationToConflict() {
        when(users.existsByUsernameIgnoreCase("new.user")).thenReturn(false);
        when(users.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$encoded");
        when(users.saveAndFlush(any(AppUser.class))).thenThrow(new DataIntegrityViolationException("unique"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.register(new AuthController.RegisterRequest(
                        "new.user", "new@example.com", "correct-horse-battery")));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }
}
