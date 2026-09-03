package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.entity.User;
import com.practice.springbootdemo.advance_performance_module.entity.VerificationCode;
import com.practice.springbootdemo.advance_performance_module.entity.VerificationPurpose;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.repository.VerificationCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.practice.springbootdemo.advance_performance_module.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private VerificationCodeRepository verificationCodeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OtpService otpService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("test@ascend.com")
                .name("Test User")
                .build();
    }

    @Test
    @DisplayName("generateAndSaveOtp: generates 6-digit OTP and hashes it for storage")
    void generateAndSaveOtp_GeneratesHashedOtp() {
        when(verificationCodeRepository.findLatestActiveCode(sampleUser.getId(), VerificationPurpose.PASSWORD_RESET))
                .thenReturn(Optional.empty());

        String rawOtp = otpService.generateAndSaveOtp(sampleUser, VerificationPurpose.PASSWORD_RESET);

        assertThat(rawOtp).hasSize(6).containsOnlyDigits();
        verify(verificationCodeRepository, times(1)).save(argThat(code -> {
            assertThat(code.getCodeHash()).hasSize(64); // SHA-256 hex
            assertThat(code.getPurpose()).isEqualTo(VerificationPurpose.PASSWORD_RESET);
            assertThat(code.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(9));
            return true;
        }));
    }

    @Test
    @DisplayName("generateAndSaveOtp: throws exception when resend is requested within 60s cooldown")
    void generateAndSaveOtp_WithinCooldown_ThrowsExceptionAndPreservesCurrentOtp() {
        VerificationCode recentCode = VerificationCode.builder()
                .id(10L)
                .createdAt(LocalDateTime.now().minusSeconds(30))
                .expiresAt(LocalDateTime.now().plusMinutes(9))
                .build();

        when(verificationCodeRepository.findLatestActiveCode(sampleUser.getId(), VerificationPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(recentCode));

        assertThatThrownBy(() -> otpService.generateAndSaveOtp(sampleUser, VerificationPurpose.PASSWORD_RESET))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Please wait");

        // Critically: Previous code is NOT invalidated during cooldown rejection
        verify(verificationCodeRepository, never()).invalidateAllActive(any(), any(), any());
        verify(verificationCodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("verifyOtp: validates correct OTP and marks as consumed atomically")
    void verifyOtp_CorrectOtp_ConsumesAtomically() {
        String rawOtp = "483921";
        String hash = OtpService.hashOtp(rawOtp);

        VerificationCode activeCode = VerificationCode.builder()
                .id(101L)
                .codeHash(hash)
                .attemptCount(0)
                .maxAttempts(5)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(verificationCodeRepository.findLatestActiveCode(sampleUser.getId(), VerificationPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(activeCode));
        when(verificationCodeRepository.consumeCode(eq(101L), any())).thenReturn(1);

        otpService.verifyOtp(sampleUser, VerificationPurpose.PASSWORD_RESET, rawOtp);

        verify(verificationCodeRepository, times(1)).consumeCode(eq(101L), any());
    }

    @Test
    @DisplayName("verifyOtp: throws exception and increments attempts on incorrect OTP")
    void verifyOtp_IncorrectOtp_IncrementsAttempts() {
        String rawOtp = "483921";
        String hash = OtpService.hashOtp(rawOtp);

        VerificationCode activeCode = VerificationCode.builder()
                .id(101L)
                .codeHash(hash)
                .attemptCount(2)
                .maxAttempts(5)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(verificationCodeRepository.findLatestActiveCode(sampleUser.getId(), VerificationPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(activeCode));

        assertThatThrownBy(() -> otpService.verifyOtp(sampleUser, VerificationPurpose.PASSWORD_RESET, "000000"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Incorrect OTP");

        verify(verificationCodeRepository, times(1)).incrementAttempts(101L);
    }

    @Test
    @DisplayName("verifyOtp: locks out user for 15 minutes upon reaching 5 failed attempts")
    void verifyOtp_MaxAttemptsReached_LocksOtpVerification() {
        String rawOtp = "483921";
        String hash = OtpService.hashOtp(rawOtp);

        VerificationCode activeCode = VerificationCode.builder()
                .id(101L)
                .codeHash(hash)
                .attemptCount(4)
                .maxAttempts(5)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(verificationCodeRepository.findLatestActiveCode(sampleUser.getId(), VerificationPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(activeCode));

        assertThatThrownBy(() -> otpService.verifyOtp(sampleUser, VerificationPurpose.PASSWORD_RESET, "000000"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Verification is locked for 15 minutes");

        assertThat(sampleUser.getOtpLockoutUntil()).isNotNull();
        assertThat(sampleUser.isOtpLocked()).isTrue();
        verify(verificationCodeRepository, times(1)).invalidateById(eq(101L), any());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    @DisplayName("verifyOtp: rejects verification immediately when user is in OTP lockout")
    void verifyOtp_UserInLockout_BlocksVerification() {
        sampleUser.setOtpLockoutUntil(LocalDateTime.now().plusMinutes(12));

        assertThatThrownBy(() -> otpService.verifyOtp(sampleUser, VerificationPurpose.PASSWORD_RESET, "123456"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Verification is locked");

        verifyNoInteractions(verificationCodeRepository);
    }

    @Test
    @DisplayName("generateAndSaveOtp: blocks OTP generation when user is in OTP lockout")
    void generateAndSaveOtp_UserInLockout_BlocksGeneration() {
        sampleUser.setOtpLockoutUntil(LocalDateTime.now().plusMinutes(10));

        assertThatThrownBy(() -> otpService.generateAndSaveOtp(sampleUser, VerificationPurpose.PASSWORD_RESET))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Too many failed verification attempts");

        verifyNoInteractions(verificationCodeRepository);
    }
}
