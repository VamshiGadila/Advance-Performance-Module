package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.entity.PasswordHistory;
import com.practice.springbootdemo.advance_performance_module.entity.User;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.repository.PasswordHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordPolicyService {

    private final PasswordService passwordService;
    private final PasswordHistoryRepository passwordHistoryRepository;

    private static final int MIN_LENGTH = 12;
    private static final int MAX_LENGTH = 128;

    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password1234", "password12345", "123456789012", "qwerty123456",
            "welcome12345", "letmein12345", "admin1234567", "changeme1234",
            "iloveyou1234", "administrator", "performance12", "ascend123456"
    );

    public void validateNewPassword(String newPassword, String confirmPassword, User user, String currentPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new BadRequestException("New password cannot be empty");
        }

        if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
            throw new BadRequestException("Passwords do not match");
        }

        if (newPassword.length() < MIN_LENGTH) {
            throw new BadRequestException("Password must be at least " + MIN_LENGTH + " characters long");
        }

        if (newPassword.length() > MAX_LENGTH) {
            throw new BadRequestException("Password cannot exceed " + MAX_LENGTH + " characters");
        }

        // Common password check
        String normalized = newPassword.toLowerCase().trim();
        if (COMMON_PASSWORDS.contains(normalized)) {
            throw new BadRequestException("This password is too common. Please choose a more secure password.");
        }


        // Regex Complexity Validation
        if (!newPassword.matches(".*[A-Z].*")) {
            throw new BadRequestException("Password must contain at least one uppercase letter (A-Z)");
        }

        if (!newPassword.matches(".*[a-z].*")) {
            throw new BadRequestException("Password must contain at least one lowercase letter (a-z)");
        }

        if (!newPassword.matches(".*\\d.*")) {
            throw new BadRequestException("Password must contain at least one number (0-9)");
        }

        if (!newPassword.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?/~`].*")) {
            throw new BadRequestException("Password must contain at least one special character (!@#$%^&*...)");
        }

        // User specific checks that require user entity
        if (user != null) {

            // Reject matching current password
            if (user.getPasswordHash() != null && passwordService.matches(newPassword, user.getPasswordHash())) {
                throw new BadRequestException("New password must be different from your current password");
            }

            // Password History check (previous 5 passwords)
            List<PasswordHistory> history = passwordHistoryRepository.findTop5ByUserIdOrderByCreatedAtDesc(user.getId());
            for (PasswordHistory ph : history) {
                if (passwordService.matches(newPassword, ph.getPasswordHash())) {
                    throw new BadRequestException("You cannot reuse any of your last 5 passwords.");
                }
            }
        }
    }

    public void recordPasswordInHistory(User user, String passwordHash) {
        if (user == null || passwordHash == null) return;
        passwordHistoryRepository.save(PasswordHistory.builder()
                .user(user)
                .passwordHash(passwordHash)
                .build());
    }
}
