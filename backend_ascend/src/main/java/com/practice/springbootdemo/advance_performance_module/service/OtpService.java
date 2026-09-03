package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.entity.User;
import com.practice.springbootdemo.advance_performance_module.entity.VerificationCode;
import com.practice.springbootdemo.advance_performance_module.entity.VerificationPurpose;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.repository.UserRepository;
import com.practice.springbootdemo.advance_performance_module.repository.VerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final VerificationCodeRepository verificationCodeRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public static final int OTP_VALIDITY_MINUTES = 10;
    public static final int RESEND_COOLDOWN_SECONDS = 60;
    public static final int MAX_VERIFICATION_ATTEMPTS = 5;
    public static final int OTP_LOCKOUT_MINUTES = 15;

    @Transactional
    public String generateAndSaveOtp(User user, VerificationPurpose purpose) {
        LocalDateTime now = LocalDateTime.now();

        // 0. Check OTP Lockout
        if (user.getOtpLockoutUntil() != null) {
            if (user.getOtpLockoutUntil().isAfter(now)) {
                long minutesLeft = Math.max(1, Duration.between(now, user.getOtpLockoutUntil()).toMinutes() + 1);
                String formattedTime = user.getOtpLockoutUntil().format(DateTimeFormatter.ofPattern("HH:mm"));
                log.warn("OTP generation blocked: User ID {} is locked out until {}", user.getId(), user.getOtpLockoutUntil());
                throw new BadRequestException("Too many failed verification attempts. Please wait " + minutesLeft + " minutes (until " + formattedTime + ") before requesting a new OTP.");
            } else {
                user.setOtpLockoutUntil(null);
                userRepository.save(user);
            }
        }

        // 1. Check for active OTP and enforce 60s Resend Cooldown
        Optional<VerificationCode> activeCodeOpt = verificationCodeRepository.findLatestActiveCode(user.getId(), purpose);
        if (activeCodeOpt.isPresent()) {
            VerificationCode activeCode = activeCodeOpt.get();
            long elapsedSeconds = Duration.between(activeCode.getCreatedAt(), now).getSeconds();
            if (elapsedSeconds < RESEND_COOLDOWN_SECONDS) {
                long waitRemaining = RESEND_COOLDOWN_SECONDS - elapsedSeconds;
                log.warn("OTP resend rejected during cooldown for user {}. Remaining: {}s", user.getId(), waitRemaining);
                throw new BadRequestException("Please wait " + waitRemaining + " seconds before requesting another OTP.");
            }

            // Cooldown passed: Invalidate previous active OTP ("Latest OTP Wins")
            verificationCodeRepository.invalidateAllActive(user.getId(), purpose, now);
            log.info("Previous active OTP invalidated for user {} and purpose {}", user.getId(), purpose);
        }

        // 2. Generate 6-digit CSPRNG OTP
        String rawOtp = String.format("%06d", secureRandom.nextInt(1_000_000));
        String codeHash = hashOtp(rawOtp);

        // 3. Persist with fresh 10-minute expiration
        VerificationCode verificationCode = VerificationCode.builder()
                .user(user)
                .purpose(purpose)
                .codeHash(codeHash)
                .attemptCount(0)
                .maxAttempts(MAX_VERIFICATION_ATTEMPTS)
                .expiresAt(now.plusMinutes(OTP_VALIDITY_MINUTES))
                .build();

        verificationCodeRepository.save(verificationCode);
        log.info("Fresh OTP issued for user ID {}, purpose {}, valid for {} minutes", user.getId(), purpose, OTP_VALIDITY_MINUTES);

        // Return raw OTP strictly for email transmission (NEVER logged or stored)
        return rawOtp;
    }

    @Transactional
    public void verifyOtp(User user, VerificationPurpose purpose, String submittedOtp) {
        if (submittedOtp == null || submittedOtp.isBlank()) {
            throw new BadRequestException("OTP code cannot be empty");
        }

        String cleanOtp = submittedOtp.trim();
        LocalDateTime now = LocalDateTime.now();

        // 0. Check OTP Lockout
        if (user.getOtpLockoutUntil() != null) {
            if (user.getOtpLockoutUntil().isAfter(now)) {
                long minutesLeft = Math.max(1, Duration.between(now, user.getOtpLockoutUntil()).toMinutes() + 1);
                String formattedTime = user.getOtpLockoutUntil().format(DateTimeFormatter.ofPattern("HH:mm"));
                log.warn("OTP verification blocked: User ID {} is locked out until {}", user.getId(), user.getOtpLockoutUntil());
                throw new BadRequestException("Too many failed OTP attempts. Verification is locked for " + minutesLeft + " minutes (until " + formattedTime + "). Please try again later.");
            } else {
                user.setOtpLockoutUntil(null);
                userRepository.save(user);
            }
        }

        // 1. Fetch latest active OTP
        VerificationCode code = verificationCodeRepository.findLatestActiveCode(user.getId(), purpose)
                .orElseThrow(() -> new BadRequestException("This OTP has expired or has already been used. Please request a new OTP."));

        // 2. Check maximum attempts
        if (code.getAttemptCount() >= code.getMaxAttempts()) {
            verificationCodeRepository.invalidateById(code.getId(), now);
            LocalDateTime lockoutExpiry = now.plusMinutes(OTP_LOCKOUT_MINUTES);
            user.setOtpLockoutUntil(lockoutExpiry);
            userRepository.save(user);
            String formattedTime = lockoutExpiry.format(DateTimeFormatter.ofPattern("HH:mm"));
            throw new BadRequestException("Maximum OTP attempts reached. Verification is locked for " + OTP_LOCKOUT_MINUTES + " minutes (until " + formattedTime + "). Please try again later.");
        }

        // 3. Check expiration
        if (code.getExpiresAt().isBefore(now)) {
            throw new BadRequestException("This OTP has expired. Please request a new OTP.");
        }

        // 4. Validate OTP Hash
        String submittedHash = hashOtp(cleanOtp);
        if (!MessageDigest.isEqual(code.getCodeHash().getBytes(StandardCharsets.UTF_8), submittedHash.getBytes(StandardCharsets.UTF_8))) {
            int newAttempts = code.getAttemptCount() + 1;
            code.setAttemptCount(newAttempts);
            verificationCodeRepository.incrementAttempts(code.getId());

            if (newAttempts >= code.getMaxAttempts()) {
                verificationCodeRepository.invalidateById(code.getId(), now);
                LocalDateTime lockoutExpiry = now.plusMinutes(OTP_LOCKOUT_MINUTES);
                user.setOtpLockoutUntil(lockoutExpiry);
                userRepository.save(user);

                String formattedTime = lockoutExpiry.format(DateTimeFormatter.ofPattern("HH:mm"));
                log.warn("OTP invalidated and user ID {} locked out from OTP verification for {} minutes (until {}) due to exceeding max attempts",
                        user.getId(), OTP_LOCKOUT_MINUTES, lockoutExpiry);
                throw new BadRequestException("Maximum OTP attempts reached. Verification is locked for " + OTP_LOCKOUT_MINUTES + " minutes (until " + formattedTime + "). Please try again later.");
            }

            int remaining = code.getMaxAttempts() - newAttempts;
            log.warn("Incorrect OTP attempt for user ID {}. Remaining attempts: {}", user.getId(), remaining);
            throw new BadRequestException("Incorrect OTP. Please try again. (" + remaining + " attempts remaining)");
        }

        // 5. Atomic Single-Use Consumption
        int updated = verificationCodeRepository.consumeCode(code.getId(), now);
        if (updated == 0) {
            log.warn("Concurrent OTP consumption blocked for code ID {}", code.getId());
            throw new BadRequestException("This OTP has already been used. Please request a new OTP.");
        }

        // 6. Clear OTP Lockout upon successful verification
        if (user.getOtpLockoutUntil() != null) {
            user.setOtpLockoutUntil(null);
            userRepository.save(user);
        }

        log.info("OTP verified and consumed successfully for user ID {} and purpose {}", user.getId(), purpose);
    }

    public static String hashOtp(String rawOtp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawOtp.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
