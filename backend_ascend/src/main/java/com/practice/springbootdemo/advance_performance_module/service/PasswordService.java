package com.practice.springbootdemo.advance_performance_module.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Primary
public class PasswordService implements PasswordEncoder {

    private final Argon2PasswordEncoder argon2PasswordEncoder;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public PasswordService() {
        // Standard Spring Security Argon2id parameters (salt length 16, hash length 32, parallelism 1, memory 16384 KiB, iterations 2)
        this.argon2PasswordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        this.bCryptPasswordEncoder = new BCryptPasswordEncoder(10);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword cannot be null");
        }
        return argon2PasswordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }

        // 1. Argon2id check (Standard for new credentials)
        if (encodedPassword.startsWith("$argon2id$") || encodedPassword.startsWith("$argon2i$")) {
            return argon2PasswordEncoder.matches(rawPassword, encodedPassword);
        }

        // 2. Backward compatibility with existing BCrypt credentials
        if (encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$") || encodedPassword.startsWith("$2y$")) {
            return bCryptPasswordEncoder.matches(rawPassword, encodedPassword);
        }

        // 3. Fallback check
        try {
            return argon2PasswordEncoder.matches(rawPassword, encodedPassword);
        } catch (Exception ex) {
            log.warn("Password hash format unrecognized: {}", ex.getMessage());
            return false;
        }
    }

    public boolean needsRehash(String encodedPassword) {
        if (encodedPassword == null) {
            return true;
        }
        // If password is still BCrypt, it needs to be upgraded to Argon2id upon successful authentication
        return !encodedPassword.startsWith("$argon2id$");
    }
}
