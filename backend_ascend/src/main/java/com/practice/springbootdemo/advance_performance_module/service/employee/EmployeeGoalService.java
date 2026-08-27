package com.practice.springbootdemo.advance_performance_module.service.employee;

import com.practice.springbootdemo.advance_performance_module.dto.employee.EmployeeGoalResponse;
import com.practice.springbootdemo.advance_performance_module.dto.goals.GoalProgressUpdateRequest;
import com.practice.springbootdemo.advance_performance_module.dto.hr.AssignmentResponse;
import com.practice.springbootdemo.advance_performance_module.entity.*;
import com.practice.springbootdemo.advance_performance_module.exception.BusinessAuthorizationException;
import com.practice.springbootdemo.advance_performance_module.exception.InvalidGoalStatusException;
import com.practice.springbootdemo.advance_performance_module.exception.ResourceNotFoundException;
import com.practice.springbootdemo.advance_performance_module.repository.GoalRepository;
import com.practice.springbootdemo.advance_performance_module.repository.ManagerAssignmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.PerformanceCycleRepository;
import com.practice.springbootdemo.advance_performance_module.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class EmployeeGoalService {
    private final GoalRepository goalRepository;
    private final PerformanceCycleRepository cycleRepository;
    private final ManagerAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;

    public EmployeeGoalService(
            GoalRepository goalRepository,
            PerformanceCycleRepository cycleRepository,
            ManagerAssignmentRepository assignmentRepository,
            UserRepository userRepository
    ) {
        this.goalRepository = goalRepository;
        this.cycleRepository = cycleRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getMyManager(Long employeeId) {
        log.info("Employee ID {} fetching assigned reporting manager", employeeId);
        return assignmentRepository.findByEmployeeIdAndActiveTrue(employeeId)
                .map(a -> {
                    User emp = userRepository.findById(a.getEmployeeId()).orElse(null);
                    User mgr = userRepository.findById(a.getManagerId()).orElse(null);
                    String cycleName = a.getPerformanceCycleId() != null
                            ? cycleRepository.findById(a.getPerformanceCycleId()).map(PerformanceCycle::getName).orElse(null)
                            : null;
                    log.debug("Found active reporting manager ID {} for Employee ID {}", a.getManagerId(), employeeId);
                    return new AssignmentResponse(
                            a.getId(),
                            a.getEmployeeId(),
                            emp != null ? emp.getName() : "Unknown",
                            emp != null ? emp.getEmployeeCode() : "-",
                            a.getManagerId(),
                            mgr != null ? mgr.getName() : "Unknown",
                            mgr != null ? mgr.getEmployeeCode() : "-",
                            a.getPerformanceCycleId(),
                            cycleName,
                            a.isActive(),
                            a.getAssignedDate() != null ? a.getAssignedDate() : a.getCreatedAt()
                    );
                })
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<EmployeeGoalResponse> getMyGoals(Long employeeId, Long cycleId) {
        log.info("Employee ID {} fetching goals (cycleId filter: {})", employeeId, cycleId);
        List<Goal> goals;
        if (cycleId != null) {
            goals = goalRepository.findByEmployeeIdAndCycleId(employeeId, cycleId);
        } else {
            PerformanceCycle activeCycle = cycleRepository.findFirstByStatusOrderByStartDateDesc(CycleStatus.ACTIVE)
                    .orElse(null);
            if (activeCycle != null) {
                goals = goalRepository.findByEmployeeIdAndCycleId(employeeId, activeCycle.getId());
                if (goals.isEmpty()) {
                    goals = goalRepository.findByEmployeeId(employeeId);
                }
            } else {
                goals = goalRepository.findByEmployeeId(employeeId);
            }
        }
        log.debug("Returning {} goals for Employee ID {}", goals.size(), employeeId);
        return goals.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeGoalResponse> getMyGoals(Long employeeId) {
        return getMyGoals(employeeId, null);
    }

    @Transactional(readOnly = true)
    public EmployeeGoalResponse getGoalById(Long goalId, Long employeeId) {
        log.debug("Employee ID {} fetching Goal ID: {}", employeeId, goalId);
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> {
                    log.error("Goal lookup failed for ID: {}", goalId);
                    return new ResourceNotFoundException("Goal not found with ID: " + goalId);
                });
        if (!goal.getEmployeeId().equals(employeeId)) {
            log.warn("Access Denied: Employee ID {} tried to view Goal ID {} owned by Employee ID {}",
                    employeeId, goalId, goal.getEmployeeId());
            throw new BusinessAuthorizationException("Business Authorization Denied: You cannot access another employee's goal");
        }
        return mapToResponse(goal);
    }

    @Transactional
    public EmployeeGoalResponse acceptGoal(Long goalId, Long employeeId) {
        log.info("Employee ID {} accepting Goal ID: {}", employeeId, goalId);
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> {
                    log.error("Acceptance failed: Goal ID {} not found", goalId);
                    return new ResourceNotFoundException("Goal not found with ID: " + goalId);
                });

        if (!goal.getEmployeeId().equals(employeeId)) {
            log.warn("Access Denied: Employee ID {} tried to accept Goal ID {} owned by Employee ID {}",
                    employeeId, goalId, goal.getEmployeeId());
            throw new BusinessAuthorizationException("Business Authorization Denied: You cannot accept another employee's goal");
        }
        if (goal.getStatus() != GoalStatus.PENDING_ACCEPTANCE) {
            log.warn("Acceptance failed: Goal ID {} is in state {}", goalId, goal.getStatus());
            throw new InvalidGoalStatusException("Goal is not in PENDING_ACCEPTANCE status (Current: " + goal.getStatus() + ")");
        }

        goal.setStatus(GoalStatus.ACCEPTED);
        goal.setEmployeeAccepted(true);
        Goal saved = goalRepository.save(goal);
        log.info("Goal ID {} successfully ACCEPTED by Employee ID {}", goalId, employeeId);
        return mapToResponse(saved);
    }

    @Transactional
    public EmployeeGoalResponse updateProgress(Long goalId, Long employeeId, GoalProgressUpdateRequest request) {
        log.info("Employee ID {} updating progress on Goal ID {} to {}%", employeeId, goalId, request.progress());
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> {
                    log.error("Progress update failed: Goal ID {} not found", goalId);
                    return new ResourceNotFoundException("Goal not found with ID: " + goalId);
                });

        if (!goal.getEmployeeId().equals(employeeId)) {
            log.warn("Access Denied: Employee ID {} tried to update progress on Goal ID {} owned by Employee ID {}",
                    employeeId, goalId, goal.getEmployeeId());
            throw new BusinessAuthorizationException("Business Authorization Denied: You cannot update progress for another employee's goal");
        }
        if (goal.getStatus() == GoalStatus.PENDING_ACCEPTANCE || goal.getStatus() == GoalStatus.REJECTED) {
            log.warn("Progress update rejected: Goal ID {} is in status {}", goalId, goal.getStatus());
            throw new InvalidGoalStatusException("Cannot update progress on a goal with status: " + goal.getStatus());
        }

        goal.setProgress(request.progress());
        if (request.comment() != null && !request.comment().isBlank()) {
            goal.setEmployeeComment(request.comment());
        }

        if (request.progress() == 100) {
            goal.setStatus(GoalStatus.COMPLETED);
            goal.setCompletedAt(LocalDateTime.now());
            log.info("Goal ID {} marked COMPLETED by Employee ID {}", goalId, employeeId);
        } else if (request.progress() > 0 && goal.getStatus() == GoalStatus.ACCEPTED) {
            goal.setStatus(GoalStatus.IN_PROGRESS);
            log.debug("Goal ID {} transitioned to IN_PROGRESS", goalId);
        }

        Goal saved = goalRepository.save(goal);
        log.info("Goal ID {} progress updated: Progress={}% Status={}", goalId, saved.getProgress(), saved.getStatus());
        return mapToResponse(saved);
    }

    private EmployeeGoalResponse mapToResponse(Goal g) {
        return new EmployeeGoalResponse(
                g.getId(),
                g.getCycleId(),
                g.getGoalType(),
                g.getGoalScope(),
                g.getTitle(),
                g.getDescription(),
                g.getTarget(),
                g.getWeight(),
                g.getDueDate(),
                g.getStatus(),
                g.getProgress(),
                g.getEmployeeComment(),
                g.getManagerComment(),
                g.isEmployeeAccepted(),
                g.isModificationRequested(),
                g.getCreatedAt(),
                g.getUpdatedAt(),
                g.getCompletedAt()
        );
    }
}