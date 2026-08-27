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
}
