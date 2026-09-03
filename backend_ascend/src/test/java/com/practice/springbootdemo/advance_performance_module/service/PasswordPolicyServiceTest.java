package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.entity.PasswordHistory;
import com.practice.springbootdemo.advance_performance_module.entity.User;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.repository.PasswordHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordPolicyServiceTest {

    @Mock
    private PasswordService passwordService;

    @Mock
    private PasswordHistoryRepository passwordHistoryRepository;

    @InjectMocks
    private PasswordPolicyService passwordPolicyService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(100L)
                .email("jane.doe@ascend.com")
                .name("Jane Doe")
                .passwordHash("$argon2id$existing_hash")
                .build();
    }

    @Test
    @DisplayName("validateNewPassword: succeeds for valid password meeting policy")
    void validateNewPassword_ValidPassword_Success() {
        String newPass = "StrongValidPassword2026!";
        when(passwordService.matches(newPass, sampleUser.getPasswordHash())).thenReturn(false);
        when(passwordHistoryRepository.findTop5ByUserIdOrderByCreatedAtDesc(100L)).thenReturn(List.of());

        assertThatCode(() -> passwordPolicyService.validateNewPassword(newPass, newPass, sampleUser, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateNewPassword: throws exception when password < 12 characters")
    void validateNewPassword_TooShort_ThrowsException() {
        assertThatThrownBy(() -> passwordPolicyService.validateNewPassword("Short123!", "Short123!", sampleUser, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least 12 characters");
    }

    @Test
    @DisplayName("validateNewPassword: throws exception when passwords do not match")
    void validateNewPassword_Mismatch_ThrowsException() {
        assertThatThrownBy(() -> passwordPolicyService.validateNewPassword("StrongPassword123!", "DifferentPassword123!", sampleUser, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Passwords do not match");
    }

    @Test
    @DisplayName("validateNewPassword: throws exception for common passwords")
    void validateNewPassword_CommonPassword_ThrowsException() {
        assertThatThrownBy(() -> passwordPolicyService.validateNewPassword("password12345", "password12345", sampleUser, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("too common");
    }

    @Test
    @DisplayName("validateNewPassword: succeeds when password contains email parts as long as regex rules are met (e.g. Hinatashoyo@123)")
    void validateNewPassword_AcceptsPasswordWithEmailUsername() {
        User dotUser = User.builder().id(2L).email("hinatashoyo.5496144@gmail.com").name("Hinata Shoyo").passwordHash("$argon2id$somehash").build();
        when(passwordService.matches("Hinatashoyo@123", dotUser.getPasswordHash())).thenReturn(false);
        when(passwordHistoryRepository.findTop5ByUserIdOrderByCreatedAtDesc(2L)).thenReturn(List.of());

        assertThatCode(() -> passwordPolicyService.validateNewPassword("Hinatashoyo@123", "Hinatashoyo@123", dotUser, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateNewPassword: throws exception when password reuses one of last 5 passwords")
    void validateNewPassword_PasswordHistoryReused_ThrowsException() {
        String newPass = "ReusedPassword123!";
        PasswordHistory oldHistory = PasswordHistory.builder().passwordHash("old_hash").build();
        when(passwordService.matches(newPass, sampleUser.getPasswordHash())).thenReturn(false);
        when(passwordHistoryRepository.findTop5ByUserIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(oldHistory));
        when(passwordService.matches(newPass, "old_hash")).thenReturn(true);

        assertThatThrownBy(() -> passwordPolicyService.validateNewPassword(newPass, newPass, sampleUser, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot reuse any of your last 5 passwords");
    }

    @Test
    @DisplayName("validateNewPassword: throws exception when password lacks uppercase letter (e.g. hinatashoyo@123)")
    void validateNewPassword_MissingUppercase_ThrowsException() {
        assertThatThrownBy(() -> passwordPolicyService.validateNewPassword("hinatashoyo@123", "hinatashoyo@123", sampleUser, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least one uppercase letter");
    }

    @Test
    @DisplayName("validateNewPassword: throws exception when password lacks lowercase letter")
    void validateNewPassword_MissingLowercase_ThrowsException() {
        assertThatThrownBy(() -> passwordPolicyService.validateNewPassword("ALLUPPERCASE123!", "ALLUPPERCASE123!", sampleUser, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least one lowercase letter");
    }

    @Test
    @DisplayName("validateNewPassword: throws exception when password lacks digit")
    void validateNewPassword_MissingDigit_ThrowsException() {
        assertThatThrownBy(() -> passwordPolicyService.validateNewPassword("NoDigitsInPassword!", "NoDigitsInPassword!", sampleUser, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least one number");
    }

    @Test
    @DisplayName("validateNewPassword: throws exception when password lacks special character")
    void validateNewPassword_MissingSpecialChar_ThrowsException() {
        assertThatThrownBy(() -> passwordPolicyService.validateNewPassword("NoSpecialChar1234", "NoSpecialChar1234", sampleUser, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least one special character");
    }
}
