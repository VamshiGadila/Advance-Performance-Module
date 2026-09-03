package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.entity.ResetAuthorization;
import com.practice.springbootdemo.advance_performance_module.entity.User;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.repository.ResetAuthorizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResetAuthorizationService {

    private final ResetAuthorizationRepository resetAuthorizationRepository;

    public static final int RESET_AUTH_VALIDITY_MINUTES = 15;

    @Transactional
    public String createResetAuthorization(User user) {
        LocalDateTime now = LocalDateTime.now();

        // Invalidate any existing unused reset authorizations for this user
        resetAuthorizationRepository.invalidateAllForUser(user.getId(), now);

        String rawToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        String tokenHash = hashToken(rawToken);

        ResetAuthorization authorization = ResetAuthorization.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(now.plusMinutes(RESET_AUTH_VALIDITY_MINUTES))
                .build();

        resetAuthorizationRepository.save(authorization);
        log.info("Issued short-lived reset authorization for user ID {}, valid for {} minutes", user.getId(), RESET_AUTH_VALIDITY_MINUTES);

        return rawToken;
    }

    @Transactional
    public User validateAndConsumeAuthorization(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException("Password reset authorization is missing. Please verify your OTP again.");
        }

        String tokenHash = hashToken(rawToken.trim());
        ResetAuthorization authorization = resetAuthorizationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("This password reset session is no longer valid. Please start again."));

        if (!authorization.isValid()) {
            throw new BadRequestException("This password reset session has expired or has already been used. Please start again.");
        }

        int updated = resetAuthorizationRepository.consumeAuthorization(authorization.getId(), LocalDateTime.now());
        if (updated == 0) {
            throw new BadRequestException("This password reset authorization has already been consumed.");
        }

        log.info("Reset authorization successfully consumed for user ID {}", authorization.getUser().getId());
        return authorization.getUser();
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
