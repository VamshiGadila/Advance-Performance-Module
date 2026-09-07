package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.entity.PasswordHistory;
import com.practice.springbootdemo.advance_performance_module.entity.User;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.repository.PasswordHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
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
        validateNewPassword(newPassword, confirmPassword, user, currentPassword, null);
    }

    public void validateNewPassword(String newPassword, String confirmPassword, User user, String currentPassword, String additionalIdentifier) {
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

        // Username / Email / Name disallowance check
        validateNotUsernameOrName(newPassword, user, additionalIdentifier);

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

    private void validateNotUsernameOrName(String password, User user, String additionalIdentifier) {
        String lowerPassword = password.toLowerCase().trim();
        Set<String> forbidden = new HashSet<>();

        if (user != null) {
            if (user.getEmail() != null) {
                String email = user.getEmail().toLowerCase().trim();
                forbidden.add(email);
                int atIdx = email.indexOf('@');
                if (atIdx > 0) {
                    forbidden.add(email.substring(0, atIdx));
                }
            }
            if (user.getName() != null) {
                forbidden.add(user.getName().toLowerCase().trim());
                for (String part : user.getName().toLowerCase().split("\\s+")) {
                    if (part.length() >= 3) {
                        forbidden.add(part);
                    }
                }
            }
            if (user.getEmployeeCode() != null) {
                forbidden.add(user.getEmployeeCode().toLowerCase().trim());
            }
        }

        if (additionalIdentifier != null && !additionalIdentifier.isBlank()) {
            String ident = additionalIdentifier.toLowerCase().trim();
            forbidden.add(ident);
            int atIdx = ident.indexOf('@');
            if (atIdx > 0) {
                forbidden.add(ident.substring(0, atIdx));
            }
            for (String part : ident.split("[\\s@._-]+")) {
                if (part.length() >= 3) {
                    forbidden.add(part);
                }
            }
        }

        for (String word : forbidden) {
            if (word.length() >= 3 && lowerPassword.contains(word)) {
                throw new BadRequestException("Password cannot contain or match your username, name, or email address ('" + word + "')");
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
