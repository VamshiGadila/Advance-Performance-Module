package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.dto.goals.GoalModificationResponse;
import com.practice.springbootdemo.advance_performance_module.dto.goals.GoalModificationSubmitRequest;
import com.practice.springbootdemo.advance_performance_module.dto.goals.ModificationReviewRequest;
import com.practice.springbootdemo.advance_performance_module.entity.*;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.exception.BusinessAuthorizationException;
import com.practice.springbootdemo.advance_performance_module.repository.GoalModificationRequestRepository;
import com.practice.springbootdemo.advance_performance_module.repository.GoalRepository;
import com.practice.springbootdemo.advance_performance_module.repository.UserRepository;
import com.practice.springbootdemo.advance_performance_module.service.goals.GoalModificationService;
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
class GoalModificationServiceTest {

    @Mock
    private GoalModificationRequestRepository requestRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GoalModificationService goalModificationService;

    private User dynamicEmployee;
    private User dynamicManager;
    private Goal dynamicGoal;
    private GoalModificationRequest dynamicModRequest;

    @BeforeEach
    void setUp() {
        dynamicEmployee = TestDataFactory.createDynamicUser(Role.EMPLOYEE);
        dynamicManager = TestDataFactory.createDynamicUser(Role.MANAGER);

        dynamicGoal = TestDataFactory.createDynamicGoal(
                dynamicEmployee.getId(),
                dynamicManager.getId(),
                1L,
                GoalType.OKR,
                null,
                GoalStatus.ACCEPTED
        );

        dynamicModRequest = TestDataFactory.createDynamicModRequest(
                dynamicGoal.getId(),
                dynamicEmployee.getId(),
                dynamicManager.getId(),
                ModificationStatus.PENDING
        );
    }

    @Test
    @DisplayName("submitRequest: should submit modification request dynamically and set goal status to MODIFICATION_REQUESTED")
    void submitRequest_Success() {
        // Arrange
        GoalModificationSubmitRequest submitRequest = new GoalModificationSubmitRequest(
                "Requesting 2-week extension", "Extend due date to Dec 15"
        );
        when(goalRepository.findById(dynamicGoal.getId())).thenReturn(Optional.of(dynamicGoal));
        when(requestRepository.existsByGoalIdAndStatus(dynamicGoal.getId(), ModificationStatus.PENDING)).thenReturn(false);
        when(requestRepository.save(any(GoalModificationRequest.class))).thenAnswer(i -> {
            GoalModificationRequest r = i.getArgument(0);
            r.setId(TestDataFactory.nextId());
            return r;
        });


        GoalModificationResponse response = goalModificationService.submitRequest(
                dynamicGoal.getId(), dynamicEmployee.getId(), submitRequest
        );


        assertThat(response).isNotNull();
        assertThat(dynamicGoal.getStatus()).isEqualTo(GoalStatus.MODIFICATION_REQUESTED);
        assertThat(dynamicGoal.isModificationRequested()).isTrue();
        verify(goalRepository, times(1)).save(dynamicGoal);
        verify(requestRepository, times(1)).save(any(GoalModificationRequest.class));
    }

    @Test
    @DisplayName("submitRequest: should throw BadRequestException when pending request already exists for this goal")
    void submitRequest_PendingRequestAlreadyExists_ThrowsBadRequestException() {

        GoalModificationSubmitRequest submitRequest = new GoalModificationSubmitRequest("Comment", "Changes");
        when(goalRepository.findById(dynamicGoal.getId())).thenReturn(Optional.of(dynamicGoal));
        when(requestRepository.existsByGoalIdAndStatus(dynamicGoal.getId(), ModificationStatus.PENDING)).thenReturn(true);


        assertThatThrownBy(() -> goalModificationService.submitRequest(
                dynamicGoal.getId(), dynamicEmployee.getId(), submitRequest
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("A pending modification request already exists for this goal");

        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("approveRequest: should approve request, set status APPROVED, and restore goal to ACCEPTED")
    void approveRequest_Success() {

        ModificationReviewRequest reviewRequest = new ModificationReviewRequest("Approved dynamic extension");
        when(requestRepository.findById(dynamicModRequest.getId())).thenReturn(Optional.of(dynamicModRequest));
        when(goalRepository.findById(dynamicGoal.getId())).thenReturn(Optional.of(dynamicGoal));
        when(requestRepository.save(any(GoalModificationRequest.class))).thenReturn(dynamicModRequest);

        GoalModificationResponse response = goalModificationService.approveRequest(
                dynamicModRequest.getId(), dynamicManager.getId(), reviewRequest
        );


        assertThat(response).isNotNull();
        assertThat(dynamicModRequest.getStatus()).isEqualTo(ModificationStatus.APPROVED);
        assertThat(dynamicModRequest.getManagerComment()).isEqualTo("Approved dynamic extension");
        assertThat(dynamicGoal.getStatus()).isEqualTo(GoalStatus.ACCEPTED);
        assertThat(dynamicGoal.isModificationRequested()).isFalse();
        verify(goalRepository, times(1)).save(dynamicGoal);
        verify(requestRepository, times(1)).save(dynamicModRequest);
    }

    @Test
    @DisplayName("approveRequest: should throw BusinessAuthorizationException when another manager attempts review")
    void approveRequest_UnauthorizedManager_ThrowsBusinessAuthorizationException() {

        long randomManagerId = TestDataFactory.nextId() + 999;
        when(requestRepository.findById(dynamicModRequest.getId())).thenReturn(Optional.of(dynamicModRequest));


        assertThatThrownBy(() -> goalModificationService.approveRequest(
                dynamicModRequest.getId(), randomManagerId, null
        ))
                .isInstanceOf(BusinessAuthorizationException.class)
                .hasMessageContaining("You are not authorized to review this modification request");

        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejectRequest: should reject request and set status REJECTED")
    void rejectRequest_Success() {

        ModificationReviewRequest reviewRequest = new ModificationReviewRequest("Cannot extend due to release schedule");
        when(requestRepository.findById(dynamicModRequest.getId())).thenReturn(Optional.of(dynamicModRequest));
        when(goalRepository.findById(dynamicGoal.getId())).thenReturn(Optional.of(dynamicGoal));
        when(requestRepository.save(any(GoalModificationRequest.class))).thenReturn(dynamicModRequest);


        GoalModificationResponse response = goalModificationService.rejectRequest(
                dynamicModRequest.getId(), dynamicManager.getId(), reviewRequest
        );


        assertThat(response).isNotNull();
        assertThat(dynamicModRequest.getStatus()).isEqualTo(ModificationStatus.REJECTED);
        assertThat(dynamicModRequest.getManagerComment()).isEqualTo("Cannot extend due to release schedule");
        assertThat(dynamicGoal.isModificationRequested()).isFalse();
        verify(goalRepository, times(1)).save(dynamicGoal);
    }
}
