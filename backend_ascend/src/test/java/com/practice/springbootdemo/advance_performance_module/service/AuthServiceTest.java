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
    private PasswordService passwordService;

    @Mock
    private PasswordPolicyService passwordPolicyService;

    @Mock
    private OtpService otpService;

    @Mock
    private ResetAuthorizationService resetAuthorizationService;

    @Mock
    private SessionService sessionService;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private SecurityAuditService securityAuditService;

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

        when(userRepository.findByEmailIgnoreCaseOrEmployeeCodeIgnoreCase(user.getEmail(), user.getEmail())).thenReturn(Optional.of(user));
        when(passwordService.matches("Password123", user.getPasswordHash())).thenReturn(true);
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
        when(userRepository.findByEmailIgnoreCaseOrEmployeeCodeIgnoreCase(dynamicUser.getEmail(), dynamicUser.getEmail())).thenReturn(Optional.of(dynamicUser));
        when(passwordService.matches("WrongPassword", dynamicUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid username or password");

        verify(jwtService, never()).generate(any());
    }

    @Test
    @DisplayName("login: should throw UnauthorizedException when account is inactive")
    void login_InactiveAccount_ThrowsUnauthorizedException() {

        dynamicUser.setActive(false);
        LoginRequest request = new LoginRequest(dynamicUser.getEmail(), "Password123");
        when(userRepository.findByEmailIgnoreCaseOrEmployeeCodeIgnoreCase(dynamicUser.getEmail(), dynamicUser.getEmail())).thenReturn(Optional.of(dynamicUser));


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
        when(passwordService.encode("SecurePass123")).thenReturn("hashed_" + dynamicEmail);
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

    @ParameterizedTest
    @ValueSource(strings = {"Kageyama12.com", "kageyama@", "@domain.com", "kageyama@domain", "plainaddress"})
    @DisplayName("signup: should throw BadRequestException when email format is invalid")
    void signup_InvalidEmailFormat_ThrowsBadRequestException(String invalidEmail) {
        SignupRequest request = new SignupRequest(
                "Dynamic Name", invalidEmail, "SecurePass123!", "SecurePass123!", dynamicDept.getId()
        );

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid email format");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("forgotPassword: generates 6-digit OTP and dispatches email for active user")
    void forgotPassword_ActiveUser_GeneratesOtpAndSendsEmail() {
        when(userRepository.findByEmailIgnoreCase("vamshigadila@gmail.com")).thenReturn(Optional.of(dynamicUser));
        when(otpService.generateAndSaveOtp(eq(dynamicUser), any())).thenReturn("123456");

        String result = authService.forgotPassword(new com.practice.springbootdemo.advance_performance_module.dto.auth.ForgotPasswordRequest("vamshigadila@gmail.com"));

        assertThat(result).contains("A verification OTP has been sent to your email.");
        verify(emailService, times(1)).sendPasswordResetOtp(eq(dynamicUser.getEmail()), eq(dynamicUser.getName()), eq("123456"));
    }

    @Test
    @DisplayName("resetPassword: successfully updates password when OTP is valid and matching")
    void resetPassword_ValidOtp_UpdatesPassword() {
        when(userRepository.findByEmailIgnoreCase("vamshigadila@gmail.com")).thenReturn(Optional.of(dynamicUser));
        when(passwordService.encode("NewSecret123")).thenReturn("hashed_new_secret");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        com.practice.springbootdemo.advance_performance_module.dto.auth.ResetPasswordRequest request =
                new com.practice.springbootdemo.advance_performance_module.dto.auth.ResetPasswordRequest(
                        "vamshigadila@gmail.com", "123456", "NewSecret123", "NewSecret123"
                );

        String result = authService.resetPassword(request);

        assertThat(result).contains("Password reset successfully");
        assertThat(dynamicUser.getPasswordHash()).isEqualTo("hashed_new_secret");
        verify(userRepository, times(1)).save(dynamicUser);
        verify(sessionService, times(1)).revokeAllSessions(dynamicUser.getId());
    }

    @Test
    @DisplayName("verifyOtp: successfully validates matching OTP")
    void verifyOtp_ValidOtp_ReturnsSuccess() {
        when(userRepository.findByEmailIgnoreCase("vamshigadila@gmail.com")).thenReturn(Optional.of(dynamicUser));
        when(resetAuthorizationService.createResetAuthorization(dynamicUser)).thenReturn("sample-reset-auth-uuid");

        com.practice.springbootdemo.advance_performance_module.dto.auth.VerifyOtpRequest request =
                new com.practice.springbootdemo.advance_performance_module.dto.auth.VerifyOtpRequest("vamshigadila@gmail.com", "654321");

        String result = authService.verifyOtp(request);
        assertThat(result).contains("OTP verified successfully");
        verify(otpService, times(1)).verifyOtp(eq(dynamicUser), eq(com.practice.springbootdemo.advance_performance_module.entity.VerificationPurpose.PASSWORD_RESET), eq("654321"));
    }

    @Test
    @DisplayName("verifyOtp: throws exception for invalid OTP")
    void verifyOtp_InvalidOtp_ThrowsException() {
        when(userRepository.findByEmailIgnoreCase("vamshigadila@gmail.com")).thenReturn(Optional.of(dynamicUser));
        doThrow(new BadRequestException("Incorrect OTP. Please try again."))
                .when(otpService).verifyOtp(eq(dynamicUser), eq(com.practice.springbootdemo.advance_performance_module.entity.VerificationPurpose.PASSWORD_RESET), eq("111111"));

        com.practice.springbootdemo.advance_performance_module.dto.auth.VerifyOtpRequest request =
                new com.practice.springbootdemo.advance_performance_module.dto.auth.VerifyOtpRequest("vamshigadila@gmail.com", "111111");

        assertThatThrownBy(() -> authService.verifyOtp(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Incorrect OTP");
    }

    @Test
    @DisplayName("resetPassword: throws exception when OTP is invalid or expired")
    void resetPassword_InvalidOtp_ThrowsException() {
        when(userRepository.findByEmailIgnoreCase("vamshigadila@gmail.com")).thenReturn(Optional.of(dynamicUser));
        doThrow(new BadRequestException("This OTP has expired."))
                .when(otpService).verifyOtp(eq(dynamicUser), eq(com.practice.springbootdemo.advance_performance_module.entity.VerificationPurpose.PASSWORD_RESET), eq("999999"));

        com.practice.springbootdemo.advance_performance_module.dto.auth.ResetPasswordRequest request =
                new com.practice.springbootdemo.advance_performance_module.dto.auth.ResetPasswordRequest(
                        "vamshigadila@gmail.com", "999999", "NewSecret123", "NewSecret123"
                );

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("This OTP has expired");
    }
}
