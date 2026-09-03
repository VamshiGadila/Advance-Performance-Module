package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.dto.hr.DeactivateEmployeeRequest;
import com.practice.springbootdemo.advance_performance_module.dto.hr.EmployeeResponse;
import com.practice.springbootdemo.advance_performance_module.entity.Role;
import com.practice.springbootdemo.advance_performance_module.entity.User;
import com.practice.springbootdemo.advance_performance_module.entity.UserStatus;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.exception.ResourceNotFoundException;
import com.practice.springbootdemo.advance_performance_module.repository.*;
import com.practice.springbootdemo.advance_performance_module.service.hr.DepartmentService;
import com.practice.springbootdemo.advance_performance_module.service.hr.EmployeeManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeManagementServiceTest {

    @Mock
    private UserRepository users;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private ManagerAssignmentRepository managerAssignmentRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserCodeGeneratorService userCodeGeneratorService;
    @Mock
    private DepartmentService departmentService;
    @Mock
    private GoalRepository goalRepository;
    @Mock
    private GoalModificationRequestRepository goalModificationRequestRepository;
    @Mock
    private UserSessionRepository userSessionRepository;
    @Mock
    private VerificationCodeRepository verificationCodeRepository;
    @Mock
    private PasswordHistoryRepository passwordHistoryRepository;
    @Mock
    private ResetAuthorizationRepository resetAuthorizationRepository;
    @Mock
    private SecurityAuditLogRepository securityAuditLogRepository;
    @Mock
    private SecurityAuditService securityAuditService;
    @Mock
    private SessionService sessionService;

    @InjectMocks
    private EmployeeManagementService employeeManagementService;

    private User sampleEmployee;
    private User sampleHr;
    private User sampleManager;

    @BeforeEach
    void setUp() {
        sampleEmployee = User.builder()
                .id(101L)
                .employeeCode("EMP101")
                .name("Alex River")
                .email("alex@ascend.local")
                .role(Role.EMPLOYEE)
                .status(UserStatus.ACTIVE)
                .active(true)
                .build();

        sampleHr = User.builder()
                .id(1L)
                .employeeCode("HR001")
                .name("HR Admin")
                .email("hr@ascend.local")
                .role(Role.HR)
                .status(UserStatus.ACTIVE)
                .active(true)
                .build();

        sampleManager = User.builder()
                .id(50L)
                .employeeCode("MGR050")
                .name("Manager Bob")
                .email("bob@ascend.local")
                .role(Role.MANAGER)
                .status(UserStatus.ACTIVE)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Deactivate employee for 24 hours successfully")
    void deactivateEmployee_ValidHours_Success() {
        when(users.findById(101L)).thenReturn(Optional.of(sampleEmployee));
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        DeactivateEmployeeRequest request = new DeactivateEmployeeRequest(24, "HOURS", "Investigation pending");
        EmployeeResponse response = employeeManagementService.deactivateEmployee(101L, request, 1L);

        assertThat(response).isNotNull();
        assertThat(sampleEmployee.getStatus()).isEqualTo(UserStatus.DISABLED);
        assertThat(sampleEmployee.isActive()).isFalse();
        assertThat(sampleEmployee.getDeactivatedUntil()).isAfter(LocalDateTime.now().plusHours(23));
        assertThat(sampleEmployee.getDeactivationReason()).isEqualTo("Investigation pending");

        verify(sessionService).revokeAllSessions(101L);
        verify(securityAuditService).recordEvent(eq(1L), eq("alex@ascend.local"), eq("EMPLOYEE_DEACTIVATED_BY_HR"), isNull(), anyString());
    }

    @Test
    @DisplayName("Deactivate employee throws BadRequestException when target is HR")
    void deactivateEmployee_AttemptOnHr_ThrowsBadRequestException() {
        when(users.findById(1L)).thenReturn(Optional.of(sampleHr));

        DeactivateEmployeeRequest request = new DeactivateEmployeeRequest(7, "DAYS", "Testing");

        assertThatThrownBy(() -> employeeManagementService.deactivateEmployee(1L, request, 2L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("HR Administrator accounts cannot be deactivated");

        verifyNoInteractions(sessionService);
    }

    @Test
    @DisplayName("Reactivate employee restores ACTIVE status and clears deactivation details")
    void reactivateEmployee_Success() {
        sampleEmployee.setStatus(UserStatus.DISABLED);
        sampleEmployee.setActive(false);
        sampleEmployee.setDeactivatedUntil(LocalDateTime.now().plusDays(5));
        sampleEmployee.setDeactivationReason("Suspended");

        when(users.findById(101L)).thenReturn(Optional.of(sampleEmployee));
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeResponse response = employeeManagementService.reactivateEmployee(101L, 1L);

        assertThat(response).isNotNull();
        assertThat(sampleEmployee.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(sampleEmployee.isActive()).isTrue();
        assertThat(sampleEmployee.getDeactivatedUntil()).isNull();
        assertThat(sampleEmployee.getDeactivationReason()).isNull();

        verify(securityAuditService).recordEvent(eq(1L), eq("alex@ascend.local"), eq("EMPLOYEE_REACTIVATED_BY_HR"), isNull(), anyString());
    }

    @Test
    @DisplayName("Permanently delete employee executes relational cascade and purges user row")
    void deleteEmployee_ValidEmployee_CascadesAndPurges() {
        when(users.findById(101L)).thenReturn(Optional.of(sampleEmployee));

        employeeManagementService.deleteEmployee(101L, 1L);

        verify(sessionService).revokeAllSessions(101L);
        verify(userSessionRepository).deleteByUserId(101L);
        verify(verificationCodeRepository).deleteByUserId(101L);
        verify(passwordHistoryRepository).deleteByUserId(101L);
        verify(resetAuthorizationRepository).deleteByUserId(101L);
        verify(managerAssignmentRepository).deleteByEmployeeId(101L);
        verify(managerAssignmentRepository).deleteByManagerId(101L);
        verify(goalModificationRequestRepository).deleteByEmployeeId(101L);
        verify(goalModificationRequestRepository).deleteByManagerId(101L);
        verify(goalRepository).deleteByEmployeeId(101L);
        verify(goalRepository).deleteByManagerId(101L);
        verify(securityAuditLogRepository).detachUser(101L);
        verify(users).delete(sampleEmployee);
        verify(securityAuditService).recordEvent(eq(1L), eq("alex@ascend.local"), eq("EMPLOYEE_DELETED_BY_HR"), isNull(), anyString());
    }

    @Test
    @DisplayName("Delete employee throws BadRequestException when trying to delete HR account")
    void deleteEmployee_AttemptOnHr_ThrowsBadRequestException() {
        when(users.findById(1L)).thenReturn(Optional.of(sampleHr));

        assertThatThrownBy(() -> employeeManagementService.deleteEmployee(1L, 2L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("HR Administrator accounts cannot be deleted");

        verify(users, never()).delete(any(User.class));
        verifyNoInteractions(sessionService);
    }

    @Test
    @DisplayName("Delete manager throws BadRequestException if manager has assigned employees")
    void deleteEmployee_ManagerWithActiveReports_ThrowsBadRequestException() {
        when(users.findById(50L)).thenReturn(Optional.of(sampleManager));
        when(managerAssignmentRepository.findAssignedEmployeeIds(50L)).thenReturn(List.of(101L, 102L));

        assertThatThrownBy(() -> employeeManagementService.deleteEmployee(50L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete manager while they have 2 active assigned employees");

        verify(users, never()).delete(any(User.class));
    }
}
