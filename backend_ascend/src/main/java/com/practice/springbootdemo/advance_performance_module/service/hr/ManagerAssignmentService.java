package com.practice.springbootdemo.advance_performance_module.service.hr;

import com.practice.springbootdemo.advance_performance_module.dto.hr.AssignManagerRequest;
import com.practice.springbootdemo.advance_performance_module.dto.hr.AssignmentResponse;
import com.practice.springbootdemo.advance_performance_module.dto.hr.UpdateAssignmentRequest;
import com.practice.springbootdemo.advance_performance_module.entity.*;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.exception.InvalidPerformanceCycleException;
import com.practice.springbootdemo.advance_performance_module.exception.ResourceNotFoundException;
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
public class ManagerAssignmentService {
    private final ManagerAssignmentRepository assignments;
    private final UserRepository users;
    private final PerformanceCycleRepository cycles;

    public ManagerAssignmentService(
            ManagerAssignmentRepository assignments,
            UserRepository users,
            PerformanceCycleRepository cycles
    ) {
        this.assignments = assignments;
        this.users = users;
        this.cycles = cycles;
    }

    @Transactional
    public AssignmentResponse assign(AssignManagerRequest request) {
        log.info("Creating manager assignment: Manager ID {} -> Employee ID {} (Cycle ID: {})",
                request.managerId(), request.employeeId(), request.performanceCycleId());

        User employee = users.findById(request.employeeId())
                .orElseThrow(() -> {
                    log.error("Assignment failed: Employee ID {} not found", request.employeeId());
                    return new ResourceNotFoundException("Employee not found with id: " + request.employeeId());
                });

        User manager = users.findById(request.managerId())
                .orElseThrow(() -> {
                    log.error("Assignment failed: Manager ID {} not found", request.managerId());
                    return new ResourceNotFoundException("Manager not found with id: " + request.managerId());
                });

        if (employee.getRole() != Role.EMPLOYEE || !employee.isActive()) {
            log.warn("Assignment rejected: User ID {} is not an active Employee", request.employeeId());
            throw new BadRequestException("Selected user is not an active Employee");
        }
        if (manager.getRole() != Role.MANAGER || !manager.isActive()) {
            log.warn("Assignment rejected: User ID {} is not an active Manager", request.managerId());
            throw new BadRequestException("Selected user is not an active Manager");
        }

        String cycleName = null;
        if (request.performanceCycleId() != null) {
            PerformanceCycle cycle = cycles.findById(request.performanceCycleId())
                    .orElseThrow(() -> {
                        log.error("Assignment failed: Performance Cycle ID {} not found", request.performanceCycleId());
                        return new ResourceNotFoundException("Performance cycle not found with id: " + request.performanceCycleId());
                    });
            if (cycle.getStatus() == CycleStatus.CLOSED) {
                log.warn("Assignment rejected: Cannot assign manager in a CLOSED cycle ID: {}", request.performanceCycleId());
                throw new InvalidPerformanceCycleException("Cannot assign manager in a CLOSED performance cycle");
            }
            cycleName = cycle.getName();
        }

        assignments.findByEmployeeIdAndActiveTrue(employee.getId())
                .ifPresent(oldAssignment -> {
                    oldAssignment.setActive(false);
                    assignments.save(oldAssignment);
                    log.info("Deactivated previous manager assignment ID {} for Employee ID {}", oldAssignment.getId(), employee.getId());
                });

        ManagerAssignment assignment = ManagerAssignment.builder()
                .employeeId(employee.getId())
                .managerId(manager.getId())
                .performanceCycleId(request.performanceCycleId())
                .active(true)
                .assignedDate(LocalDateTime.now())
                .build();

        ManagerAssignment saved = assignments.save(assignment);
        log.info("Manager assignment created successfully: ID={}, Manager ID {} -> Employee ID {}", saved.getId(), manager.getId(), employee.getId());
        return map(saved, employee, manager, cycleName);
    }

    @Transactional
    public AssignmentResponse update(Long id, UpdateAssignmentRequest request) {
        log.info("Updating Manager Assignment ID: {}", id);
        ManagerAssignment assignment = assignments.findById(id)
                .orElseThrow(() -> {
                    log.error("Update failed: Manager Assignment ID {} not found", id);
                    return new ResourceNotFoundException("Manager assignment not found with id: " + id);
                });

        User manager = users.findById(request.managerId())
                .orElseThrow(() -> {
                    log.error("Update failed: Manager ID {} not found", request.managerId());
                    return new ResourceNotFoundException("Manager not found with id: " + request.managerId());
                });

        if (manager.getRole() != Role.MANAGER || !manager.isActive()) {
            log.warn("Update rejected: User ID {} is not an active Manager", request.managerId());
            throw new BadRequestException("Selected user is not an active Manager");
        }

        User employee = users.findById(assignment.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + assignment.getEmployeeId()));

        String cycleName = null;
        if (request.performanceCycleId() != null) {
            PerformanceCycle cycle = cycles.findById(request.performanceCycleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Performance cycle not found with id: " + request.performanceCycleId()));
            assignment.setPerformanceCycleId(request.performanceCycleId());
            cycleName = cycle.getName();
        }

        assignment.setManagerId(request.managerId());
        if (request.active() != null) {
            assignment.setActive(request.active());
        }

        ManagerAssignment saved = assignments.save(assignment);
        log.info("Manager assignment updated: ID={}, Manager ID={}, Active={}", saved.getId(), saved.getManagerId(), saved.isActive());
        return map(saved, employee, manager, cycleName);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deactivating Manager Assignment ID: {}", id);
        ManagerAssignment assignment = assignments.findById(id)
                .orElseThrow(() -> {
                    log.error("Deactivation failed: Manager Assignment ID {} not found", id);
                    return new ResourceNotFoundException("Manager assignment not found with id: " + id);
                });
        assignment.setActive(false);
        assignments.save(assignment);
        log.info("Manager assignment ID {} deactivated", id);
    }

    public AssignmentResponse getById(Long id) {
        log.debug("Fetching Manager Assignment ID: {}", id);
        ManagerAssignment a = assignments.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manager assignment not found with id: " + id));
        User emp = users.findById(a.getEmployeeId()).orElse(null);
        User mgr = users.findById(a.getManagerId()).orElse(null);
        String cycleName = a.getPerformanceCycleId() != null
                ? cycles.findById(a.getPerformanceCycleId()).map(PerformanceCycle::getName).orElse(null)
                : null;
        return map(a, emp, mgr, cycleName);
    }

    public List<AssignmentResponse> getAssignments() {
        log.debug("Retrieving all active manager assignments");
        List<AssignmentResponse> list = assignments.findByActiveTrue().stream()
                .map(a -> {
                    User emp = users.findById(a.getEmployeeId()).orElse(null);
                    User mgr = users.findById(a.getManagerId()).orElse(null);
                    String cycleName = a.getPerformanceCycleId() != null
                            ? cycles.findById(a.getPerformanceCycleId()).map(PerformanceCycle::getName).orElse(null)
                            : null;
                    return map(a, emp, mgr, cycleName);
                })
                .toList();
        log.debug("Found {} active manager assignments", list.size());
        return list;
    }

    private AssignmentResponse map(ManagerAssignment a, User employee, User manager, String cycleName) {
        return new AssignmentResponse(
                a.getId(),
                a.getEmployeeId(),
                employee != null ? employee.getName() : "Unknown",
                employee != null ? employee.getEmployeeCode() : "-",
                a.getManagerId(),
                manager != null ? manager.getName() : "Unknown",
                manager != null ? manager.getEmployeeCode() : "-",
                a.getPerformanceCycleId(),
                cycleName,
                a.isActive(),
                a.getAssignedDate() != null ? a.getAssignedDate() : a.getCreatedAt()
        );
    }
}