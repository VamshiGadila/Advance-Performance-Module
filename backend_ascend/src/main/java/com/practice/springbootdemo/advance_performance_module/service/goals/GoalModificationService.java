package com.practice.springbootdemo.advance_performance_module.service.goals;

import com.practice.springbootdemo.advance_performance_module.dto.goals.GoalModificationResponse;
import com.practice.springbootdemo.advance_performance_module.dto.goals.GoalModificationSubmitRequest;
import com.practice.springbootdemo.advance_performance_module.dto.goals.ModificationReviewRequest;
import com.practice.springbootdemo.advance_performance_module.entity.*;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.exception.BusinessAuthorizationException;
import com.practice.springbootdemo.advance_performance_module.exception.ResourceNotFoundException;
import com.practice.springbootdemo.advance_performance_module.repository.GoalModificationRequestRepository;
import com.practice.springbootdemo.advance_performance_module.repository.GoalRepository;
import com.practice.springbootdemo.advance_performance_module.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class GoalModificationService {
    private final GoalModificationRequestRepository requestRepository;
    private final GoalRepository goalRepository;
    private final UserRepository userRepository;

    public GoalModificationService(
            GoalModificationRequestRepository requestRepository,
            GoalRepository goalRepository,
            UserRepository userRepository
    ) {
        this.requestRepository = requestRepository;
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public GoalModificationResponse submitRequest(Long goalId, Long employeeId, GoalModificationSubmitRequest submitRequest) {
        log.info("Employee ID {} submitting modification request for Goal ID: {}", employeeId, goalId);

        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> {
                    log.error("Goal not found for modification request: ID {}", goalId);
                    return new ResourceNotFoundException("Goal not found with ID: " + goalId);
                });

        if (!goal.getEmployeeId().equals(employeeId)) {
            log.warn("Modification submission denied: Employee ID {} does not own Goal ID {}", employeeId, goalId);
            throw new BusinessAuthorizationException("Business Authorization Denied: You can only request modification for your own goals");
        }
        if (goal.getStatus() == GoalStatus.COMPLETED) {
            log.warn("Modification submission rejected: Goal ID {} is already COMPLETED", goalId);
            throw new BadRequestException("Cannot request modification for a COMPLETED goal");
        }
        if (requestRepository.existsByGoalIdAndStatus(goalId, ModificationStatus.PENDING)) {
            log.warn("Modification submission rejected: Pending request already exists for Goal ID {}", goalId);
            throw new BadRequestException("A pending modification request already exists for this goal");
        }

        goal.setStatus(GoalStatus.MODIFICATION_REQUESTED);
        goal.setModificationRequested(true);
        goal.setEmployeeComment(submitRequest.comment());
        goalRepository.save(goal);

        GoalModificationRequest modRequest = GoalModificationRequest.builder()
                .goalId(goal.getId())
                .employeeId(employeeId)
                .managerId(goal.getManagerId())
                .requestedChanges(submitRequest.requestedChanges())
                .comment(submitRequest.comment())
                .status(ModificationStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        GoalModificationRequest saved = requestRepository.save(modRequest);
        log.info("Goal modification request created successfully: ID={}, Goal ID={}, Status=PENDING", saved.getId(), goalId);
        return mapToResponse(saved, goal.getTitle());
    }

    @Transactional
    public GoalModificationResponse approveRequest(Long requestId, Long managerId, ModificationReviewRequest reviewRequest) {
        log.info("Manager ID {} reviewing APPROVAL for Modification Request ID: {}", managerId, requestId);

        GoalModificationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.error("Modification Request ID {} not found", requestId);
                    return new ResourceNotFoundException("Goal modification request not found with ID: " + requestId);
                });

        if (!request.getManagerId().equals(managerId)) {
            log.warn("Authorization Denied: Manager ID {} is not the assigned manager for Request ID {}", managerId, requestId);
            throw new BusinessAuthorizationException("Business Authorization Denied: You are not authorized to review this modification request");
        }
        if (request.getStatus() != ModificationStatus.PENDING) {
            log.warn("Approval rejected: Request ID {} is already in status {}", requestId, request.getStatus());
            throw new BadRequestException("Modification request has already been reviewed (Status: " + request.getStatus() + ")");
        }

        request.setStatus(ModificationStatus.APPROVED);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(managerId);
        if (reviewRequest != null && reviewRequest.comment() != null) {
            request.setManagerComment(reviewRequest.comment());
        }

        Goal goal = goalRepository.findById(request.getGoalId())
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with ID: " + request.getGoalId()));

        goal.setStatus(GoalStatus.ACCEPTED);
        goal.setModificationRequested(false);
        if (reviewRequest != null && reviewRequest.comment() != null) {
            goal.setManagerComment(reviewRequest.comment());
        }
        goalRepository.save(goal);

        GoalModificationRequest saved = requestRepository.save(request);
        log.info("Modification Request ID {} successfully APPROVED by Manager ID {}", requestId, managerId);
        return mapToResponse(saved, goal.getTitle());
    }

    @Transactional
    public GoalModificationResponse rejectRequest(Long requestId, Long managerId, ModificationReviewRequest reviewRequest) {
        log.info("Manager ID {} reviewing REJECTION for Modification Request ID: {}", managerId, requestId);

        GoalModificationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.error("Modification Request ID {} not found", requestId);
                    return new ResourceNotFoundException("Goal modification request not found with ID: " + requestId);
                });

        if (!request.getManagerId().equals(managerId)) {
            log.warn("Authorization Denied: Manager ID {} is not the assigned manager for Request ID {}", managerId, requestId);
            throw new BusinessAuthorizationException("Business Authorization Denied: You are not authorized to review this modification request");
        }
        if (request.getStatus() != ModificationStatus.PENDING) {
            log.warn("Rejection failed: Request ID {} has already been reviewed", requestId);
            throw new BadRequestException("Modification request has already been reviewed");
        }

        request.setStatus(ModificationStatus.REJECTED);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(managerId);
        if (reviewRequest != null && reviewRequest.comment() != null) {
            request.setManagerComment(reviewRequest.comment());
        }

        Goal goal = goalRepository.findById(request.getGoalId())
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with ID: " + request.getGoalId()));

        goal.setStatus(goal.isEmployeeAccepted() ? GoalStatus.ACCEPTED : GoalStatus.PENDING_ACCEPTANCE);
        goal.setModificationRequested(false);
        if (reviewRequest != null && reviewRequest.comment() != null) {
            goal.setManagerComment(reviewRequest.comment());
        }
        goalRepository.save(goal);

        GoalModificationRequest saved = requestRepository.save(request);
        log.info("Modification Request ID {} successfully REJECTED by Manager ID {}", requestId, managerId);
        return mapToResponse(saved, goal.getTitle());
    }

    @Transactional(readOnly = true)
    public List<GoalModificationResponse> getManagerModificationRequests(Long managerId, ModificationStatus status) {
        log.debug("Manager ID {} fetching modification requests (status filter: {})", managerId, status);
        List<GoalModificationRequest> list = (status != null)
                ? requestRepository.findByManagerIdAndStatus(managerId, status)
                : requestRepository.findByManagerId(managerId);
        log.debug("Found {} modification requests for Manager ID {}", list.size(), managerId);
        return list.stream().map(r -> {
            String title = goalRepository.findById(r.getGoalId()).map(Goal::getTitle).orElse("Goal #" + r.getGoalId());
            return mapToResponse(r, title);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<GoalModificationResponse> getEmployeeModificationRequests(Long employeeId) {
        log.debug("Employee ID {} fetching submitted modification requests", employeeId);
        return requestRepository.findByEmployeeId(employeeId).stream()
                .map(r -> {
                    String title = goalRepository.findById(r.getGoalId()).map(Goal::getTitle).orElse("Goal #" + r.getGoalId());
                    return mapToResponse(r, title);
                })
                .toList();
    }

    private GoalModificationResponse mapToResponse(GoalModificationRequest r, String goalTitle) {
        String employeeName = userRepository.findById(r.getEmployeeId()).map(User::getName).orElse("Unknown");
        String managerName = userRepository.findById(r.getManagerId()).map(User::getName).orElse("Unknown");
        return new GoalModificationResponse(
                r.getId(),
                r.getGoalId(),
                goalTitle,
                r.getEmployeeId(),
                employeeName,
                r.getManagerId(),
                managerName,
                r.getRequestedChanges(),
                r.getComment(),
                r.getStatus(),
                r.getManagerComment(),
                r.getRequestedAt(),
                r.getReviewedAt()
        );
    }
}