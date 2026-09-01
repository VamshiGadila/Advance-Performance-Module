package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.dto.auth.LoginRequest;
import com.practice.springbootdemo.advance_performance_module.dto.auth.LoginResponse;
import com.practice.springbootdemo.advance_performance_module.dto.auth.SignupRequest;
import com.practice.springbootdemo.advance_performance_module.dto.auth.SignupResponse;
import com.practice.springbootdemo.advance_performance_module.entity.Department;
import com.practice.springbootdemo.advance_performance_module.entity.Role;
import com.practice.springbootdemo.advance_performance_module.entity.User;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.exception.UnauthorizedException;
import com.practice.springbootdemo.advance_performance_module.repository.DepartmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.ManagerAssignmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.UserRepository;
import com.practice.springbootdemo.advance_performance_module.security.JwtService;
import com.practice.springbootdemo.advance_performance_module.security.TokenBlacklistService;
import com.practice.springbootdemo.advance_performance_module.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ManagerAssignmentRepository managerAssignmentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserCodeGeneratorService userCodeGeneratorService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User dynamicUser;
    private Department dynamicDept;

    @BeforeEach
    void setUp() {
        dynamicDept = TestDataFactory.createDynamicDepartment();
        dynamicUser = TestDataFactory.createDynamicUser(Role.EMPLOYEE);
        dynamicUser.setDepartmentId(dynamicDept.getId());
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    @DisplayName("login: should authenticate and return token for any dynamic role (HR, MANAGER, EMPLOYEE)")
    void login_DynamicRoles_Success(Role role) {

        User user = TestDataFactory.createDynamicUser(role);
        user.setDepartmentId(dynamicDept.getId());
        LoginRequest request = new LoginRequest(user.getEmail(), "Password123");

        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", user.getPasswordHash())).thenReturn(true);
        when(jwtService.generate(user)).thenReturn("jwt.token." + user.getId());
        when(departmentRepository.findById(dynamicDept.getId())).thenReturn(Optional.of(dynamicDept));

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("jwt.token." + user.getId());
        assertThat(response.email()).isEqualTo(user.getEmail());
        assertThat(response.role()).isEqualTo(role);
        assertThat(response.departmentName()).isEqualTo(dynamicDept.getName());
        verify(jwtService, times(1)).generate(user);
    }

    @Test
    @DisplayName("login: should throw UnauthorizedException when password does not match")
    void login_InvalidPassword_ThrowsUnauthorizedException() {

        LoginRequest request = new LoginRequest(dynamicUser.getEmail(), "WrongPassword");
        when(userRepository.findByEmailIgnoreCase(dynamicUser.getEmail())).thenReturn(Optional.of(dynamicUser));
        when(passwordEncoder.matches("WrongPassword", dynamicUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");

        verify(jwtService, never()).generate(any());
    }

    @Test
    @DisplayName("login: should throw UnauthorizedException when account is inactive")
    void login_InactiveAccount_ThrowsUnauthorizedException() {

        dynamicUser.setActive(false);
        LoginRequest request = new LoginRequest(dynamicUser.getEmail(), "Password123");
        when(userRepository.findByEmailIgnoreCase(dynamicUser.getEmail())).thenReturn(Optional.of(dynamicUser));


        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Your account is inactive");
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev1@ascend.local", "qa.lead@ascend.local", "cloud.architect@ascend.local"})
    @DisplayName("signup: should dynamically register new employee with unique codes and emails")
    void signup_DynamicEmails_Success(String dynamicEmail) {

        long deptId = dynamicDept.getId();
        SignupRequest request = new SignupRequest(
                "Dynamic User", dynamicEmail, "SecurePass123", "SecurePass123", deptId
        );
        when(userRepository.existsByEmailIgnoreCase(dynamicEmail.toLowerCase())).thenReturn(false);
        when(departmentRepository.findById(deptId)).thenReturn(Optional.of(dynamicDept));
        when(userCodeGeneratorService.generateEmployeeCode()).thenReturn("EMP" + TestDataFactory.nextId());
        when(passwordEncoder.encode("SecurePass123")).thenReturn("hashed_" + dynamicEmail);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(TestDataFactory.nextId());
            return u;
        });


        SignupResponse response = authService.signup(request);


        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo(dynamicEmail.toLowerCase());
        assertThat(response.departmentId()).isEqualTo(deptId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("signup: should throw BadRequestException when passwords do not match")
    void signup_PasswordMismatch_ThrowsBadRequestException() {

        SignupRequest request = new SignupRequest(
                "Dynamic Name", "user@ascend.local", "Pass1", "Pass2", dynamicDept.getId()
        );

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Passwords do not match");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("signup: should throw BadRequestException when email already exists")
    void signup_EmailAlreadyExists_ThrowsBadRequestException() {

        String existingEmail = dynamicUser.getEmail();
        SignupRequest request = new SignupRequest(
                "Dynamic Name", existingEmail, "Pass1", "Pass1", dynamicDept.getId()
        );
        when(userRepository.existsByEmailIgnoreCase(existingEmail.toLowerCase())).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email is already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("forgotPassword: generates 6-digit OTP and dispatches email for active user")
    void forgotPassword_ActiveUser_GeneratesOtpAndSendsEmail() {
        when(userRepository.findByEmailIgnoreCase("vamshigadila@gmail.com")).thenReturn(Optional.of(dynamicUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = authService.forgotPassword(new com.practice.springbootdemo.advance_performance_module.dto.auth.ForgotPasswordRequest("vamshigadila@gmail.com"));

        assertThat(result).contains("Password reset OTP has been sent");
        verify(emailService, times(1)).sendPasswordResetOtp(eq(dynamicUser.getEmail()), eq(dynamicUser.getName()), anyString());
        verify(userRepository, times(1)).save(dynamicUser);
        assertThat(dynamicUser.getResetToken()).isNotNull();
        assertThat(dynamicUser.getResetToken()).hasSize(6);
    }

    @Test
    @DisplayName("resetPassword: successfully updates password when OTP is valid and matching")
    void resetPassword_ValidOtp_UpdatesPassword() {
        dynamicUser.setResetToken("123456");
        dynamicUser.setResetTokenExpiry(java.time.LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmailIgnoreCase("vamshigadila@gmail.com")).thenReturn(Optional.of(dynamicUser));
        when(passwordEncoder.encode("NewSecret123")).thenReturn("hashed_new_secret");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        com.practice.springbootdemo.advance_performance_module.dto.auth.ResetPasswordRequest request =
                new com.practice.springbootdemo.advance_performance_module.dto.auth.ResetPasswordRequest(
                        "vamshigadila@gmail.com", "123456", "NewSecret123", "NewSecret123"
                );

        String result = authService.resetPassword(request);

        assertThat(result).contains("Password has been successfully updated");
        assertThat(dynamicUser.getPasswordHash()).isEqualTo("hashed_new_secret");
        assertThat(dynamicUser.getResetToken()).isNull();
        verify(userRepository, times(1)).save(dynamicUser);
    }

    @Test
    @DisplayName("verifyOtp: successfully validates matching OTP")
    void verifyOtp_ValidOtp_ReturnsSuccess() {
        dynamicUser.setResetToken("654321");
        dynamicUser.setResetTokenExpiry(java.time.LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmailIgnoreCase("vamshigadila@gmail.com")).thenReturn(Optional.of(dynamicUser));

        com.practice.springbootdemo.advance_performance_module.dto.auth.VerifyOtpRequest request =
                new com.practice.springbootdemo.advance_performance_module.dto.auth.VerifyOtpRequest("vamshigadila@gmail.com", "654321");

        String result = authService.verifyOtp(request);
        assertThat(result).contains("OTP code verified successfully");
    }

    @Test
    @DisplayName("verifyOtp: throws exception for invalid OTP")
    void verifyOtp_InvalidOtp_ThrowsException() {
        dynamicUser.setResetToken("654321");
        dynamicUser.setResetTokenExpiry(java.time.LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmailIgnoreCase("vamshigadila@gmail.com")).thenReturn(Optional.of(dynamicUser));

        com.practice.springbootdemo.advance_performance_module.dto.auth.VerifyOtpRequest request =
                new com.practice.springbootdemo.advance_performance_module.dto.auth.VerifyOtpRequest("vamshigadila@gmail.com", "111111");

        assertThatThrownBy(() -> authService.verifyOtp(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid OTP code");
    }

    @Test
    @DisplayName("resetPassword: throws exception when OTP is invalid or expired")
    void resetPassword_InvalidOtp_ThrowsException() {
        dynamicUser.setResetToken("123456");
        dynamicUser.setResetTokenExpiry(java.time.LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmailIgnoreCase("vamshigadila@gmail.com")).thenReturn(Optional.of(dynamicUser));

        com.practice.springbootdemo.advance_performance_module.dto.auth.ResetPasswordRequest request =
                new com.practice.springbootdemo.advance_performance_module.dto.auth.ResetPasswordRequest(
                        "vamshigadila@gmail.com", "999999", "NewSecret123", "NewSecret123"
                );

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid OTP code");
    }
}
