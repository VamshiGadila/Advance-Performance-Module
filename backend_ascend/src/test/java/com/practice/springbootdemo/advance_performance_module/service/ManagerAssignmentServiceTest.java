package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.dto.hr.AssignManagerRequest;
import com.practice.springbootdemo.advance_performance_module.dto.hr.AssignmentResponse;
import com.practice.springbootdemo.advance_performance_module.entity.*;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.exception.ResourceNotFoundException;
import com.practice.springbootdemo.advance_performance_module.repository.ManagerAssignmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.PerformanceCycleRepository;
import com.practice.springbootdemo.advance_performance_module.repository.UserRepository;
import com.practice.springbootdemo.advance_performance_module.service.hr.ManagerAssignmentService;
import com.practice.springbootdemo.advance_performance_module.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagerAssignmentServiceTest {

    @Mock
    private ManagerAssignmentRepository assignmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PerformanceCycleRepository cycleRepository;

    @InjectMocks
    private ManagerAssignmentService managerAssignmentService;

    private User dynamicEmployee;
    private User dynamicManager;
    private PerformanceCycle dynamicActiveCycle;

    @BeforeEach
    void setUp() {
        dynamicEmployee = TestDataFactory.createDynamicUser(Role.EMPLOYEE);
        dynamicManager = TestDataFactory.createDynamicUser(Role.MANAGER);
        dynamicActiveCycle = TestDataFactory.createDynamicCycle(CycleStatus.ACTIVE);
    }

    @Test
    @DisplayName("assign: should map employee to manager and deactivate old assignment dynamically")
    void assignManager_Success() {
        // Arrange
        AssignManagerRequest request = new AssignManagerRequest(
                dynamicEmployee.getId(), dynamicManager.getId(), dynamicActiveCycle.getId()
        );
        ManagerAssignment oldAssignment = TestDataFactory.createDynamicAssignment(
                dynamicEmployee.getId(), TestDataFactory.nextId(), dynamicActiveCycle.getId()
        );

        when(userRepository.findById(dynamicEmployee.getId())).thenReturn(Optional.of(dynamicEmployee));
        when(userRepository.findById(dynamicManager.getId())).thenReturn(Optional.of(dynamicManager));
        when(cycleRepository.findById(dynamicActiveCycle.getId())).thenReturn(Optional.of(dynamicActiveCycle));
        when(assignmentRepository.findByEmployeeIdAndActiveTrue(dynamicEmployee.getId())).thenReturn(Optional.of(oldAssignment));
        when(assignmentRepository.save(any(ManagerAssignment.class))).thenAnswer(i -> {
            ManagerAssignment a = i.getArgument(0);
            if (a.getId() == null) a.setId(TestDataFactory.nextId());
            return a;
        });


        AssignmentResponse response = managerAssignmentService.assign(request);


        assertThat(response).isNotNull();
        assertThat(response.employeeId()).isEqualTo(dynamicEmployee.getId());
        assertThat(response.managerId()).isEqualTo(dynamicManager.getId());
        assertThat(oldAssignment.isActive()).isFalse(); // Verifies old assignment deactivation
        verify(assignmentRepository, atLeastOnce()).save(any(ManagerAssignment.class));
    }

    @Test
    @DisplayName("assign: should throw BadRequestException when selected manager does not have MANAGER role")
    void assignManager_InvalidManagerRole_ThrowsBadRequestException() {

        User nonManager = TestDataFactory.createDynamicUser(Role.EMPLOYEE);
        AssignManagerRequest request = new AssignManagerRequest(
                dynamicEmployee.getId(), nonManager.getId(), dynamicActiveCycle.getId()
        );

        when(userRepository.findById(dynamicEmployee.getId())).thenReturn(Optional.of(dynamicEmployee));
        when(userRepository.findById(nonManager.getId())).thenReturn(Optional.of(nonManager));


        assertThatThrownBy(() -> managerAssignmentService.assign(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Selected user is not an active Manager");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("assign: should throw ResourceNotFoundException when employee is not found")
    void assignManager_EmployeeNotFound_ThrowsResourceNotFoundException() {

        long randomInvalidId = TestDataFactory.nextId() + 9999;
        AssignManagerRequest request = new AssignManagerRequest(
                randomInvalidId, dynamicManager.getId(), dynamicActiveCycle.getId()
        );
        when(userRepository.findById(randomInvalidId)).thenReturn(Optional.empty());


        assertThatThrownBy(() -> managerAssignmentService.assign(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found with id: " + randomInvalidId);
    }
}
