package com.practice.springbootdemo.advance_performance_module.service.manager;

import com.practice.springbootdemo.advance_performance_module.dto.hr.EmployeeResponse;
import com.practice.springbootdemo.advance_performance_module.dto.manager.CreateGoalRequest;
import com.practice.springbootdemo.advance_performance_module.dto.manager.GoalResponse;
import com.practice.springbootdemo.advance_performance_module.dto.manager.UpdateGoalRequest;
import com.practice.springbootdemo.advance_performance_module.entity.*;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.exception.BusinessAuthorizationException;
import com.practice.springbootdemo.advance_performance_module.exception.InvalidPerformanceCycleException;
import com.practice.springbootdemo.advance_performance_module.exception.ResourceNotFoundException;
import com.practice.springbootdemo.advance_performance_module.repository.DepartmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.GoalRepository;
import com.practice.springbootdemo.advance_performance_module.repository.ManagerAssignmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.PerformanceCycleRepository;
import com.practice.springbootdemo.advance_performance_module.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ManagerGoalService {
    private final GoalRepository goalRepository;
    private final ManagerAssignmentRepository assignmentRepository;
    private final PerformanceCycleRepository cycleRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public ManagerGoalService(
            GoalRepository goalRepository,
            ManagerAssignmentRepository assignmentRepository,
            PerformanceCycleRepository cycleRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository
    ) {
        this.goalRepository = goalRepository;
        this.assignmentRepository = assignmentRepository;
        this.cycleRepository = cycleRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getMyTeam(Long managerId) {
        log.info("Manager ID {} fetching assigned direct reports", managerId);
        List<ManagerAssignment> assignments = assignmentRepository.findByManagerIdAndActiveTrue(managerId);
        List<EmployeeResponse> team = assignments.stream()
                .map(a -> userRepository.findById(a.getEmployeeId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(User::isActive)
                .map(this::mapToEmployeeResponse)
                .toList();
        log.debug("Found {} active direct reports for Manager ID {}", team.size(), managerId);
        return team;
    }

    @Transactional
    public GoalResponse create(CreateGoalRequest request, Long managerId) {
        log.info("Manager ID {} assigning Goal '{}' (Weight: {}%) to Employee ID {} in Cycle ID {}",
                managerId, request.title(), request.weight(), request.employeeId(), request.cycleId());

        boolean assigned = assignmentRepository.existsByEmployeeIdAndManagerIdAndActiveTrue(request.employeeId(), managerId);
        if (!assigned) {
            log.warn("Authorization Denied: Manager ID {} is not assigned to Employee ID {}", managerId, request.employeeId());
            throw new BusinessAuthorizationException("Business Authorization Denied: You are not assigned as this employee's manager");
        }

        PerformanceCycle cycle = cycleRepository.findById(request.cycleId())
                .orElseThrow(() -> {
                    log.error("Performance Cycle ID {} not found", request.cycleId());
                    return new ResourceNotFoundException("Performance cycle not found with ID: " + request.cycleId());
                });

        if (cycle.getStatus() != CycleStatus.ACTIVE) {
            log.warn("Goal creation rejected: Cycle ID {} is not ACTIVE (Current: {})", request.cycleId(), cycle.getStatus());
            throw new InvalidPerformanceCycleException("Goals can only be created in an ACTIVE performance cycle (Current: " + cycle.getStatus() + ")");
        }

        BigDecimal currentTotal = goalRepository.totalWeight(request.employeeId(), request.cycleId());
        BigDecimal newTotal = currentTotal.add(request.weight());
        if (newTotal.compareTo(BigDecimal.valueOf(100.00)) > 0) {
            log.warn("Weight validation failed: Employee ID {} total weight would exceed 100% (Current: {}%, Requested: {}%)",
                    request.employeeId(), currentTotal, request.weight());
            throw new BadRequestException(String.format("Total weight cannot exceed 100.00%%. Current: %.2f%%, Requested: %.2f%%", currentTotal, request.weight()));
        }

        Goal goal = Goal.builder()
                .cycleId(request.cycleId())
                .employeeId(request.employeeId())
                .managerId(managerId)
                .goalType(request.goalType())
                .goalScope(request.goalScope() != null ? request.goalScope() : GoalScope.INDIVIDUAL)
                .parentGoalId(request.parentGoalId())
                .title(request.title().trim())
                .description(request.description())
                .target(request.target())
                .weight(request.weight())
                .dueDate(request.dueDate())
                .status(GoalStatus.PENDING_ACCEPTANCE)
                .progress(0)
                .employeeAccepted(false)
                .modificationRequested(false)
                .build();

        Goal saved = goalRepository.save(goal);
        log.info("Goal created successfully: ID={}, Status={}, Assigned to Employee ID {}", saved.getId(), saved.getStatus(), request.employeeId());
        return mapToResponse(saved);
    }

    @Transactional
    public GoalResponse update(Long goalId, UpdateGoalRequest request, Long managerId) {
        log.info("Manager ID {} updating Goal ID: {}", managerId, goalId);
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> {
                    log.error("Goal update failed: Goal ID {} not found", goalId);
                    return new ResourceNotFoundException("Goal not found with ID: " + goalId);
                });

        if (!goal.getManagerId().equals(managerId)) {
            log.warn("Authorization Denied: Manager ID {} tried to update Goal ID {} owned by Manager ID {}",
                    managerId, goalId, goal.getManagerId());
            throw new BusinessAuthorizationException("Business Authorization Denied: You can only update goals created by you");
        }
        if (goal.getStatus() == GoalStatus.COMPLETED) {
            log.warn("Update rejected: Cannot update COMPLETED Goal ID: {}", goalId);
            throw new BadRequestException("Cannot update a COMPLETED goal");
        }

        BigDecimal currentTotalOther = goalRepository.totalWeightExcluding(goal.getEmployeeId(), goal.getCycleId(), goal.getId());
        BigDecimal newTotal = currentTotalOther.add(request.weight());
        if (newTotal.compareTo(BigDecimal.valueOf(100.00)) > 0) {
            log.warn("Weight validation failed on update: Total weight would exceed 100% (Other: {}%, Requested: {}%)",
                    currentTotalOther, request.weight());
            throw new BadRequestException(String.format("Total weight exceeds 100.00%%. Current other goals: %.2f%%, Requested: %.2f%%", currentTotalOther, request.weight()));
        }

        goal.setTitle(request.title().trim());
        goal.setDescription(request.description());
        goal.setTarget(request.target());
        goal.setWeight(request.weight());
        goal.setDueDate(request.dueDate());
        Goal saved = goalRepository.save(goal);
        log.info("Goal ID {} updated successfully: Title='{}', Weight={}%", goalId, saved.getTitle(), saved.getWeight());
        return mapToResponse(saved);
    }

    @Transactional
    public void delete(Long goalId, Long managerId) {
        log.info("Manager ID {} attempting to delete Goal ID: {}", managerId, goalId);
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> {
                    log.error("Delete failed: Goal ID {} not found", goalId);
                    return new ResourceNotFoundException("Goal not found with ID: " + goalId);
                });

        if (!goal.getManagerId().equals(managerId)) {
            log.warn("Authorization Denied: Manager ID {} tried to delete Goal ID {} created by Manager ID {}",
                    managerId, goalId, goal.getManagerId());
            throw new BusinessAuthorizationException("Business Authorization Denied: You cannot delete another manager's goal");
        }
        if (goal.getStatus() == GoalStatus.COMPLETED || goal.getProgress() > 0) {
            log.warn("Delete rejected: Goal ID {} has recorded progress ({}%) or is COMPLETED", goalId, goal.getProgress());
            throw new BadRequestException("Cannot delete a goal with recorded progress or COMPLETED status");
        }

        goalRepository.delete(goal);
        log.info("Goal ID {} successfully deleted by Manager ID {}", goalId, managerId);
    }

    @Transactional(readOnly = true)
    public GoalResponse getGoal(Long goalId, Long managerId) {
        log.debug("Manager ID {} fetching Goal ID: {}", managerId, goalId);
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with ID: " + goalId));
        if (!goal.getManagerId().equals(managerId)) {
            throw new BusinessAuthorizationException("Business Authorization Denied: You do not have access to this goal");
        }
        return mapToResponse(goal);
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> getAllManagerGoals(Long managerId) {
        log.debug("Manager ID {} fetching all created goals", managerId);
        return goalRepository.findByManagerId(managerId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> getEmployeeGoals(Long employeeId, Long cycleId, Long managerId) {
        log.debug("Manager ID {} fetching goals for Employee ID {} in Cycle ID {}", managerId, employeeId, cycleId);
        boolean assigned = assignmentRepository.existsByEmployeeIdAndManagerIdAndActiveTrue(employeeId, managerId);
        if (!assigned) {
            throw new BusinessAuthorizationException("Business Authorization Denied: You are not assigned to this employee");
        }
        return goalRepository.findByEmployeeIdAndCycleId(employeeId, cycleId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private EmployeeResponse mapToEmployeeResponse(User user) {
        String deptName = (user.getDepartmentId() != null)
                ? departmentRepository.findById(user.getDepartmentId()).map(Department::getName).orElse(null)
                : null;
        return new EmployeeResponse(
                user.getId(),
                user.getEmployeeCode(),
                user.getName(),
                user.getEmail(),
                user.getDepartmentId(),
                deptName,
                user.getRole(),
                user.getSkill(),
                user.getLocation(),
                user.getDomain(),
                user.getExperienceYears(),
                user.isActive()
        );
    }

    private GoalResponse mapToResponse(Goal g) {
        String employeeName = userRepository.findById(g.getEmployeeId()).map(User::getName).orElse("Unknown");
        String managerName = userRepository.findById(g.getManagerId()).map(User::getName).orElse("Unknown");
        return new GoalResponse(
                g.getId(),
                g.getCycleId(),
                g.getEmployeeId(),
                employeeName,
                g.getManagerId(),
                managerName,
                g.getGoalType(),
                g.getGoalScope(),
                g.getParentGoalId(),
                g.getTitle(),
                g.getDescription(),
                g.getTarget(),
                g.getWeight(),
                g.getDueDate(),
                g.getStatus(),
                g.getProgress(),
                g.getEmployeeComment(),
                g.getManagerComment(),
                g.isModificationRequested(),
                g.isEmployeeAccepted(),
                g.getCreatedAt(),
                g.getUpdatedAt(),
                g.getCompletedAt()
        );
    }
}