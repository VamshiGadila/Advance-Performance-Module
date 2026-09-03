package com.practice.springbootdemo.advance_performance_module.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordServiceTest {

    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordService();
    }

    @Test
    @DisplayName("encode: generates valid Argon2id hash")
    void encode_GeneratesArgon2idHash() {
        String raw = "StrongPassword123!";
        String encoded = passwordService.encode(raw);

        assertThat(encoded).startsWith("$argon2id$");
        assertThat(passwordService.matches(raw, encoded)).isTrue();
        assertThat(passwordService.matches("WrongPassword123!", encoded)).isFalse();
    }

    @Test
    @DisplayName("matches: transparently verifies legacy BCrypt hashes")
    void matches_VerifiesLegacyBCryptHash() {
        BCryptPasswordEncoder bCrypt = new BCryptPasswordEncoder(10);
        String raw = "LegacySecret123!";
        String bCryptHash = bCrypt.encode(raw);

        assertThat(bCryptHash).startsWith("$2a$");
        assertThat(passwordService.matches(raw, bCryptHash)).isTrue();
        assertThat(passwordService.matches("WrongPass!", bCryptHash)).isFalse();
    }

    @Test
    @DisplayName("needsRehash: returns true for BCrypt and false for Argon2id")
    void needsRehash_DetectsLegacyHashes() {
        BCryptPasswordEncoder bCrypt = new BCryptPasswordEncoder(10);
        String bCryptHash = bCrypt.encode("OldPass");
        String argon2Hash = passwordService.encode("NewPass");

        assertThat(passwordService.needsRehash(bCryptHash)).isTrue();
        assertThat(passwordService.needsRehash(argon2Hash)).isFalse();
    }
}
