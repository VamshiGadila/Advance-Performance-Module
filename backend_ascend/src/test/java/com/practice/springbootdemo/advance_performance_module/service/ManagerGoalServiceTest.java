package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.dto.manager.CreateGoalRequest;
import com.practice.springbootdemo.advance_performance_module.dto.manager.GoalResponse;
import com.practice.springbootdemo.advance_performance_module.dto.manager.UpdateGoalRequest;
import com.practice.springbootdemo.advance_performance_module.entity.*;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.exception.BusinessAuthorizationException;
import com.practice.springbootdemo.advance_performance_module.exception.InvalidPerformanceCycleException;
import com.practice.springbootdemo.advance_performance_module.repository.GoalRepository;
import com.practice.springbootdemo.advance_performance_module.repository.ManagerAssignmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.PerformanceCycleRepository;
import com.practice.springbootdemo.advance_performance_module.repository.UserRepository;
import com.practice.springbootdemo.advance_performance_module.service.manager.ManagerGoalService;
import com.practice.springbootdemo.advance_performance_module.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagerGoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private ManagerAssignmentRepository assignmentRepository;

    @Mock
    private PerformanceCycleRepository cycleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.practice.springbootdemo.advance_performance_module.repository.DepartmentRepository departmentRepository;

    @InjectMocks
    private ManagerGoalService managerGoalService;

    private User dynamicManager;
    private User dynamicEmployee;
    private PerformanceCycle dynamicActiveCycle;
    private Goal dynamicGoal;

    @BeforeEach
    void setUp() {
        dynamicManager = TestDataFactory.createDynamicUser(Role.MANAGER);
        dynamicEmployee = TestDataFactory.createDynamicUser(Role.EMPLOYEE);
        dynamicActiveCycle = TestDataFactory.createDynamicCycle(CycleStatus.ACTIVE);

        dynamicGoal = TestDataFactory.createDynamicGoal(
                dynamicEmployee.getId(),
                dynamicManager.getId(),
                dynamicActiveCycle.getId(),
                GoalType.OKR,
                new BigDecimal("25.00"),
                GoalStatus.PENDING_ACCEPTANCE
        );
    }

    @ParameterizedTest
    @EnumSource(GoalType.class)
    @DisplayName("create: should allocate goal dynamically for both OKR and KPI types")
    void createGoal_DynamicGoalTypes_Success(GoalType type) {

        CreateGoalRequest request = new CreateGoalRequest(
                dynamicActiveCycle.getId(), dynamicEmployee.getId(), type, GoalScope.INDIVIDUAL, null,
                "Dynamic Deliverable " + type, "Core deliverables",
                "Metric SLA", new BigDecimal("35.00"), LocalDate.now().plusMonths(3)
        );

        when(assignmentRepository.existsByEmployeeIdAndManagerIdAndActiveTrue(dynamicEmployee.getId(), dynamicManager.getId())).thenReturn(true);
        when(cycleRepository.findById(dynamicActiveCycle.getId())).thenReturn(Optional.of(dynamicActiveCycle));
        when(goalRepository.totalWeight(dynamicEmployee.getId(), dynamicActiveCycle.getId())).thenReturn(new BigDecimal("40.00")); // 40 + 35 = 75 <= 100
        when(goalRepository.save(any(Goal.class))).thenAnswer(i -> {
            Goal g = i.getArgument(0);
            g.setId(TestDataFactory.nextId());
            return g;
        });


        GoalResponse response = managerGoalService.create(request, dynamicManager.getId());


        assertThat(response).isNotNull();
        assertThat(response.goalType()).isEqualTo(type);
        assertThat(response.weight()).isEqualTo(new BigDecimal("35.00"));
        verify(goalRepository, times(1)).save(any(Goal.class));
    }

    @ParameterizedTest
    @CsvSource({
            "20.00, 30.00, true",   // 20 + 30 = 50 <= 100 -> Valid
            "50.00, 50.00, true",   // 50 + 50 = 100 <= 100 -> Valid boundary
            "80.00, 25.00, false",  // 80 + 25 = 105 > 100 -> Invalid
            "99.00, 2.00, false"    // 99 + 2 = 101 > 100 -> Invalid
    })
    @DisplayName("create: should dynamically validate total weight ceiling constraint (<= 100%)")
    void createGoal_DynamicWeightCalculations(String existingWeightStr, String newWeightStr, boolean shouldSucceed) {

        BigDecimal existingWeight = new BigDecimal(existingWeightStr);
        BigDecimal newWeight = new BigDecimal(newWeightStr);

        CreateGoalRequest request = new CreateGoalRequest(
                dynamicActiveCycle.getId(), dynamicEmployee.getId(), GoalType.OKR, GoalScope.INDIVIDUAL, null,
                "Dynamic Weight Goal", "Desc", "Target", newWeight, null
        );

        when(assignmentRepository.existsByEmployeeIdAndManagerIdAndActiveTrue(dynamicEmployee.getId(), dynamicManager.getId())).thenReturn(true);
        when(cycleRepository.findById(dynamicActiveCycle.getId())).thenReturn(Optional.of(dynamicActiveCycle));
        when(goalRepository.totalWeight(dynamicEmployee.getId(), dynamicActiveCycle.getId())).thenReturn(existingWeight);

        if (shouldSucceed) {
            when(goalRepository.save(any(Goal.class))).thenAnswer(i -> {
                Goal g = i.getArgument(0);
                g.setId(TestDataFactory.nextId());
                return g;
            });


            GoalResponse response = managerGoalService.create(request, dynamicManager.getId());


            assertThat(response).isNotNull();
            assertThat(response.weight()).isEqualTo(newWeight);
            verify(goalRepository, times(1)).save(any(Goal.class));
        } else {

            assertThatThrownBy(() -> managerGoalService.create(request, dynamicManager.getId()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Total weight cannot exceed 100.00%");

            verify(goalRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("create: should throw BusinessAuthorizationException if manager is not assigned to employee")
    void createGoal_NotAssignedManager_ThrowsBusinessAuthorizationException() {

        CreateGoalRequest request = new CreateGoalRequest(
                dynamicActiveCycle.getId(), dynamicEmployee.getId(), GoalType.OKR, GoalScope.INDIVIDUAL, null,
                "Title", null, null, new BigDecimal("30.00"), null
        );
        when(assignmentRepository.existsByEmployeeIdAndManagerIdAndActiveTrue(dynamicEmployee.getId(), dynamicManager.getId())).thenReturn(false);


        assertThatThrownBy(() -> managerGoalService.create(request, dynamicManager.getId()))
                .isInstanceOf(BusinessAuthorizationException.class)
                .hasMessageContaining("You are not assigned as this employee's manager");

        verify(goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("create: should throw InvalidPerformanceCycleException if cycle is not ACTIVE")
    void createGoal_CycleNotActive_ThrowsInvalidPerformanceCycleException() {

        dynamicActiveCycle.setStatus(CycleStatus.CLOSED);
        CreateGoalRequest request = new CreateGoalRequest(
                dynamicActiveCycle.getId(), dynamicEmployee.getId(), GoalType.OKR, GoalScope.INDIVIDUAL, null,
                "Title", null, null, new BigDecimal("30.00"), null
        );
        when(assignmentRepository.existsByEmployeeIdAndManagerIdAndActiveTrue(dynamicEmployee.getId(), dynamicManager.getId())).thenReturn(true);
        when(cycleRepository.findById(dynamicActiveCycle.getId())).thenReturn(Optional.of(dynamicActiveCycle));


        assertThatThrownBy(() -> managerGoalService.create(request, dynamicManager.getId()))
                .isInstanceOf(InvalidPerformanceCycleException.class)
                .hasMessageContaining("Goals can only be created in an ACTIVE performance cycle");
    }

    @Test
    @DisplayName("update: should update dynamic goal details successfully")
    void updateGoal_Success() {

        UpdateGoalRequest request = new UpdateGoalRequest(
                "Updated Dynamic Title", "Updated description",
                "Updated target metric", new BigDecimal("40.00"),
                LocalDate.now().plusMonths(4)
        );
        when(goalRepository.findById(dynamicGoal.getId())).thenReturn(Optional.of(dynamicGoal));
        when(goalRepository.totalWeightExcluding(dynamicGoal.getEmployeeId(), dynamicGoal.getCycleId(), dynamicGoal.getId()))
                .thenReturn(new BigDecimal("50.00")); // 50 + 40 = 90 <= 100
        when(goalRepository.save(any(Goal.class))).thenReturn(dynamicGoal);


        GoalResponse response = managerGoalService.update(dynamicGoal.getId(), request, dynamicManager.getId());


        assertThat(response).isNotNull();
        assertThat(dynamicGoal.getTitle()).isEqualTo("Updated Dynamic Title");
        verify(goalRepository, times(1)).save(dynamicGoal);
    }

    @Test
    @DisplayName("delete: should delete goal when not completed and has 0 progress")
    void deleteGoal_Success() {

        when(goalRepository.findById(dynamicGoal.getId())).thenReturn(Optional.of(dynamicGoal));


        managerGoalService.delete(dynamicGoal.getId(), dynamicManager.getId());


        verify(goalRepository, times(1)).delete(dynamicGoal);
    }

    @Test
    @DisplayName("delete: should throw BadRequestException when goal has recorded progress > 0")
    void deleteGoal_WithProgress_ThrowsBadRequestException() {

        dynamicGoal.setProgress(35);
        when(goalRepository.findById(dynamicGoal.getId())).thenReturn(Optional.of(dynamicGoal));


        assertThatThrownBy(() -> managerGoalService.delete(dynamicGoal.getId(), dynamicManager.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete a goal with recorded progress");

        verify(goalRepository, never()).delete(any(Goal.class));
    }
}
