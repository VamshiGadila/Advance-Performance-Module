package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.dto.employee.EmployeeGoalResponse;
import com.practice.springbootdemo.advance_performance_module.dto.goals.GoalProgressUpdateRequest;
import com.practice.springbootdemo.advance_performance_module.entity.*;
import com.practice.springbootdemo.advance_performance_module.exception.BusinessAuthorizationException;
import com.practice.springbootdemo.advance_performance_module.exception.InvalidGoalStatusException;
import com.practice.springbootdemo.advance_performance_module.repository.GoalRepository;
import com.practice.springbootdemo.advance_performance_module.repository.PerformanceCycleRepository;
import com.practice.springbootdemo.advance_performance_module.service.employee.EmployeeGoalService;
import com.practice.springbootdemo.advance_performance_module.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeGoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private PerformanceCycleRepository cycleRepository;

    @Mock
    private com.practice.springbootdemo.advance_performance_module.repository.ManagerAssignmentRepository assignmentRepository;

    @Mock
    private com.practice.springbootdemo.advance_performance_module.repository.UserRepository userRepository;

    @InjectMocks
    private EmployeeGoalService employeeGoalService;

    private User dynamicEmployee;
    private User dynamicManager;
    private PerformanceCycle dynamicActiveCycle;
    private Goal dynamicGoal;

    @BeforeEach
    void setUp() {
        dynamicEmployee = TestDataFactory.createDynamicUser(Role.EMPLOYEE);
        dynamicManager = TestDataFactory.createDynamicUser(Role.MANAGER);
        dynamicActiveCycle = TestDataFactory.createDynamicCycle(CycleStatus.ACTIVE);

        dynamicGoal = TestDataFactory.createDynamicGoal(
                dynamicEmployee.getId(),
                dynamicManager.getId(),
                dynamicActiveCycle.getId(),
                GoalType.OKR,
                new BigDecimal("30.00"),
                GoalStatus.PENDING_ACCEPTANCE
        );
    }

    @Test
    @DisplayName("getMyGoals: should return employee goals for the dynamic active review cycle")
    void getMyGoals_Success() {

        when(cycleRepository.findFirstByStatusOrderByStartDateDesc(CycleStatus.ACTIVE))
                .thenReturn(Optional.of(dynamicActiveCycle));
        when(goalRepository.findByEmployeeIdAndCycleId(dynamicEmployee.getId(), dynamicActiveCycle.getId()))
                .thenReturn(List.of(dynamicGoal));


        List<EmployeeGoalResponse> result = employeeGoalService.getMyGoals(dynamicEmployee.getId());


        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo(dynamicGoal.getTitle());
        assertThat(result.get(0).status()).isEqualTo(GoalStatus.PENDING_ACCEPTANCE);
    }

    @Test
    @DisplayName("acceptGoal: should dynamically transition goal to ACCEPTED")
    void acceptGoal_Success() {

        when(goalRepository.findById(dynamicGoal.getId())).thenReturn(Optional.of(dynamicGoal));
        when(goalRepository.save(any(Goal.class))).thenReturn(dynamicGoal);


        EmployeeGoalResponse response = employeeGoalService.acceptGoal(dynamicGoal.getId(), dynamicEmployee.getId());


        assertThat(response).isNotNull();
        assertThat(dynamicGoal.getStatus()).isEqualTo(GoalStatus.ACCEPTED);
        assertThat(dynamicGoal.isEmployeeAccepted()).isTrue();
        verify(goalRepository, times(1)).save(dynamicGoal);
    }

    @Test
    @DisplayName("acceptGoal: should throw BusinessAuthorizationException when another employee attempts acceptance")
    void acceptGoal_NotOwner_ThrowsBusinessAuthorizationException() {

        when(goalRepository.findById(dynamicGoal.getId())).thenReturn(Optional.of(dynamicGoal));


        long anotherEmpId = TestDataFactory.nextId();
        assertThatThrownBy(() -> employeeGoalService.acceptGoal(dynamicGoal.getId(), anotherEmpId))
                .isInstanceOf(BusinessAuthorizationException.class)
                .hasMessageContaining("You cannot accept another employee's goal");

        verify(goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("acceptGoal: should throw InvalidGoalStatusException when goal is not PENDING_ACCEPTANCE")
    void acceptGoal_InvalidStatus_ThrowsInvalidGoalStatusException() {

        dynamicGoal.setStatus(GoalStatus.ACCEPTED);
        when(goalRepository.findById(dynamicGoal.getId())).thenReturn(Optional.of(dynamicGoal));


        assertThatThrownBy(() -> employeeGoalService.acceptGoal(dynamicGoal.getId(), dynamicEmployee.getId()))
                .isInstanceOf(InvalidGoalStatusException.class)
                .hasMessageContaining("Goal is not in PENDING_ACCEPTANCE status");
    }

    @ParameterizedTest
    @CsvSource({
            "25, IN_PROGRESS, Milestone 1 finished",
            "50, IN_PROGRESS, Halfway through deliverables",
            "80, IN_PROGRESS, Final testing phase",
            "100, COMPLETED, Fully verified and released"
    })
    @DisplayName("updateProgress: should dynamically transition goal status based on progress percentage")
    void updateProgress_DynamicProgress_TransitionsStatus(int progress, GoalStatus expectedStatus, String comment) {
        // Arrange
        dynamicGoal.setStatus(GoalStatus.ACCEPTED);
        GoalProgressUpdateRequest request = new GoalProgressUpdateRequest(progress, comment);
        when(goalRepository.findById(dynamicGoal.getId())).thenReturn(Optional.of(dynamicGoal));
        when(goalRepository.save(any(Goal.class))).thenReturn(dynamicGoal);


        EmployeeGoalResponse response = employeeGoalService.updateProgress(dynamicGoal.getId(), dynamicEmployee.getId(), request);


        assertThat(response).isNotNull();
        assertThat(dynamicGoal.getProgress()).isEqualTo(progress);
        assertThat(dynamicGoal.getStatus()).isEqualTo(expectedStatus);
        assertThat(dynamicGoal.getEmployeeComment()).isEqualTo(comment);
        if (progress == 100) {
            assertThat(dynamicGoal.getCompletedAt()).isNotNull();
        }
        verify(goalRepository, times(1)).save(dynamicGoal);
    }

    @Test
    @DisplayName("getMyManager: should return assigned manager details when active assignment exists")
    void getMyManager_ActiveAssignment_ReturnsManagerResponse() {
        ManagerAssignment assignment = TestDataFactory.createDynamicAssignment(
                dynamicEmployee.getId(), dynamicManager.getId(), dynamicActiveCycle.getId()
        );

        when(assignmentRepository.findByEmployeeIdAndActiveTrue(dynamicEmployee.getId()))
                .thenReturn(Optional.of(assignment));
        when(userRepository.findById(dynamicEmployee.getId())).thenReturn(Optional.of(dynamicEmployee));
        when(userRepository.findById(dynamicManager.getId())).thenReturn(Optional.of(dynamicManager));
        when(cycleRepository.findById(dynamicActiveCycle.getId())).thenReturn(Optional.of(dynamicActiveCycle));

        com.practice.springbootdemo.advance_performance_module.dto.hr.AssignmentResponse response =
                employeeGoalService.getMyManager(dynamicEmployee.getId());

        assertThat(response).isNotNull();
        assertThat(response.employeeId()).isEqualTo(dynamicEmployee.getId());
        assertThat(response.managerId()).isEqualTo(dynamicManager.getId());
        assertThat(response.managerName()).isEqualTo(dynamicManager.getName());
    }

    @Test
    @DisplayName("getMyManager: should return null when no active manager assignment exists")
    void getMyManager_NoAssignment_ReturnsNull() {
        when(assignmentRepository.findByEmployeeIdAndActiveTrue(dynamicEmployee.getId()))
                .thenReturn(Optional.empty());

        com.practice.springbootdemo.advance_performance_module.dto.hr.AssignmentResponse response =
                employeeGoalService.getMyManager(dynamicEmployee.getId());

        assertThat(response).isNull();
    }
}
