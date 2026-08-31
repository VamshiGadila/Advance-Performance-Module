package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.entity.AuthProvider;
import com.practice.springbootdemo.advance_performance_module.entity.Department;
import com.practice.springbootdemo.advance_performance_module.entity.Role;
import com.practice.springbootdemo.advance_performance_module.entity.User;
import com.practice.springbootdemo.advance_performance_module.exception.UnauthorizedException;
import com.practice.springbootdemo.advance_performance_module.repository.DepartmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.ManagerAssignmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ManagerAssignmentRepository managerAssignmentRepository;

    @Mock
    private UserCodeGeneratorService userCodeGeneratorService;

    @InjectMocks
    private OAuth2AuthService oAuth2AuthService;

    @Test
    @DisplayName("Process existing user preserves existing role and updates Google provider ID without duplicates")
    void testProcessOAuth2User_ExistingUser_PreservesRole() {
        User existing = User.builder()
                .id(100L)
                .email("manager@ascend.local")
                .employeeCode("MGR001")
                .name("Existing Manager")
                .role(Role.MANAGER)
                .active(true)
                .build();

        when(userRepository.findByEmailIgnoreCase("manager@ascend.local")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = oAuth2AuthService.processOAuth2User("manager@ascend.local", "Updated Name", "google-sub-12345");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getRole()).isEqualTo(Role.MANAGER);
        assertThat(result.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(result.getProviderId()).isEqualTo("google-sub-12345");
        verify(userRepository, never()).findByEmployeeCode(anyString());
    }

    @Test
    @DisplayName("Process new user automatically provisions EMPLOYEE account with generated code")
    void testProcessOAuth2User_NewUser_CreatesEmployee() {
        when(userRepository.findByEmailIgnoreCase("newemployee@gmail.com")).thenReturn(Optional.empty());
        when(userCodeGeneratorService.generateEmployeeCode()).thenReturn("EMP099");
        Department dept = Department.builder().id(1L).name("Engineering").build();
        when(departmentRepository.findAll()).thenReturn(List.of(dept));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(200L);
            return u;
        });

        User result = oAuth2AuthService.processOAuth2User("newemployee@gmail.com", "Jane Doe", "google-sub-999");

        assertThat(result).isNotNull();
        assertThat(result.getEmployeeCode()).isEqualTo("EMP099");
        assertThat(result.getRole()).isEqualTo(Role.EMPLOYEE);
        assertThat(result.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(result.getEmail()).isEqualTo("newemployee@gmail.com");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("Inactive or locked user is blocked from OAuth login")
    void testProcessOAuth2User_InactiveUser_ThrowsException() {
        User inactive = User.builder()
                .id(101L)
                .email("inactive@ascend.local")
                .active(false)
                .build();

        when(userRepository.findByEmailIgnoreCase("inactive@ascend.local")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> oAuth2AuthService.processOAuth2User("inactive@ascend.local", "Inactive User", "sub-123"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("inactive");
    }
}