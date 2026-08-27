package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.dto.analytics.*;
import com.practice.springbootdemo.advance_performance_module.entity.*;
import com.practice.springbootdemo.advance_performance_module.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
public class AnalyticsService {
    private final UserRepository userRepository;
    private final PerformanceCycleRepository cycleRepository;
    private final ManagerAssignmentRepository assignmentRepository;
    private final GoalRepository goalRepository;
    private final GoalModificationRequestRepository modRepository;
    private final DepartmentRepository departmentRepository;

    public AnalyticsService(
            UserRepository userRepository,
            PerformanceCycleRepository cycleRepository,
            ManagerAssignmentRepository assignmentRepository,
            GoalRepository goalRepository,
            GoalModificationRequestRepository modRepository,
            DepartmentRepository departmentRepository
    ) {
        this.userRepository = userRepository;
        this.cycleRepository = cycleRepository;
        this.assignmentRepository = assignmentRepository;
        this.goalRepository = goalRepository;
        this.modRepository = modRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public HrAnalyticsResponse getHrAnalytics() {
        log.debug("Compiling organization-wide HR analytics summary");
        long totalEmployees = userRepository.count();
        long totalManagers = userRepository.findByRoleAndActiveTrue(Role.MANAGER).size();
        long activeAssignments = assignmentRepository.findByActiveTrue().size();
        long totalCycles = cycleRepository.count();

        Optional<PerformanceCycle> activeCycle = cycleRepository.findFirstByStatusOrderByStartDateDesc(CycleStatus.ACTIVE);
        Long activeCycleId = activeCycle.map(PerformanceCycle::getId).orElse(null);
        String cycleName = activeCycle.map(PerformanceCycle::getName).orElse(null);
        String startDate = activeCycle.map(c -> c.getStartDate().toString()).orElse(null);
        String endDate = activeCycle.map(c -> c.getEndDate().toString()).orElse(null);

        List<Goal> cycleGoals = activeCycleId != null ? goalRepository.findByCycleId(activeCycleId) : goalRepository.findAll();
        long totalGoals = cycleGoals.size();
        long completedGoals = cycleGoals.stream().filter(g -> g.getStatus() == GoalStatus.COMPLETED).count();
        double completionRate = totalGoals > 0 ? Math.round((completedGoals * 100.0 / totalGoals) * 10.0) / 10.0 : 0.0;

        Map<String, Long> statusMap = new LinkedHashMap<>();
        for (GoalStatus s : GoalStatus.values()) {
            statusMap.put(s.name(), cycleGoals.stream().filter(g -> g.getStatus() == s).count());
        }

        List<Department> departments = departmentRepository.findAll();
        List<DepartmentMetric> deptMetrics = departments.stream().map(d -> {
            List<User> deptUsers = userRepository.findByDepartmentIdAndActiveTrue(d.getId());
            long empCount = deptUsers.size();
            List<Long> empIds = deptUsers.stream().map(User::getId).toList();
            List<Goal> deptGoals = cycleGoals.stream().filter(g -> empIds.contains(g.getEmployeeId())).toList();
            long deptTotal = deptGoals.size();
            long deptCompleted = deptGoals.stream().filter(g -> g.getStatus() == GoalStatus.COMPLETED).count();
            double deptRate = deptTotal > 0 ? Math.round((deptCompleted * 100.0 / deptTotal) * 10.0) / 10.0 : 0.0;
            return new DepartmentMetric(d.getId(), d.getName(), empCount, deptRate);
        }).toList();

        return new HrAnalyticsResponse(
                totalEmployees,
                totalManagers,
                activeAssignments,
                totalCycles,
                activeCycleId,
                cycleName,
                startDate,
                endDate,
                totalGoals,
                completedGoals,
                completionRate,
                statusMap,
                deptMetrics
        );
    }

    @Transactional(readOnly = true)
    public ManagerAnalyticsResponse getManagerAnalytics(Long managerId) {
        log.debug("Compiling team analytics for Manager ID {}", managerId);
        List<Long> assignedEmpIds = assignmentRepository.findAssignedEmployeeIds(managerId);
        long teamSize = assignedEmpIds.size();

        Optional<PerformanceCycle> activeCycle = cycleRepository.findFirstByStatusOrderByStartDateDesc(CycleStatus.ACTIVE);
        Long activeCycleId = activeCycle.map(PerformanceCycle::getId).orElse(null);

        List<Goal> teamGoals = (activeCycleId != null)
                ? goalRepository.findByManagerIdAndCycleId(managerId, activeCycleId)
                : goalRepository.findByManagerId(managerId);

        long totalGoals = teamGoals.size();
        long completed = teamGoals.stream().filter(g -> g.getStatus() == GoalStatus.COMPLETED).count();
        long inProgress = teamGoals.stream().filter(g -> g.getStatus() == GoalStatus.IN_PROGRESS).count();
        long pending = teamGoals.stream().filter(g -> g.getStatus() == GoalStatus.PENDING_ACCEPTANCE).count();

        double teamCompletionRate = totalGoals > 0 ? Math.round((completed * 100.0 / totalGoals) * 10.0) / 10.0 : 0.0;
        long pendingMods = modRepository.findByManagerIdAndStatus(managerId, ModificationStatus.PENDING).size();

        Map<String, Long> statusMap = new LinkedHashMap<>();
        for (GoalStatus s : GoalStatus.values()) {
            statusMap.put(s.name(), teamGoals.stream().filter(g -> g.getStatus() == s).count());
        }

        return new ManagerAnalyticsResponse(
                teamSize,
                totalGoals,
                completed,
                inProgress,
                pending,
                teamCompletionRate,
                pendingMods,
                statusMap
        );
    }

    @Transactional(readOnly = true)
    public EmployeeAnalyticsResponse getEmployeeAnalytics(Long employeeId) {
        log.debug("Compiling personal analytics for Employee ID {}", employeeId);
        Optional<PerformanceCycle> activeCycle = cycleRepository.findFirstByStatusOrderByStartDateDesc(CycleStatus.ACTIVE);
        Long activeCycleId = activeCycle.map(PerformanceCycle::getId).orElse(null);
        String cycleName = activeCycle.map(PerformanceCycle::getName).orElse("No Active Cycle");

        List<Goal> myGoals = (activeCycleId != null)
                ? goalRepository.findByEmployeeIdAndCycleId(employeeId, activeCycleId)
                : goalRepository.findByEmployeeId(employeeId);

        long totalGoals = myGoals.size();
        BigDecimal totalWeight = activeCycleId != null ? goalRepository.totalWeight(employeeId, activeCycleId) : BigDecimal.ZERO;
        long completed = myGoals.stream().filter(g -> g.getStatus() == GoalStatus.COMPLETED).count();
        long inProgress = myGoals.stream().filter(g -> g.getStatus() == GoalStatus.IN_PROGRESS).count();
        long pending = myGoals.stream().filter(g -> g.getStatus() == GoalStatus.PENDING_ACCEPTANCE).count();

        double avgProgress = totalGoals > 0
                ? Math.round((myGoals.stream().mapToInt(Goal::getProgress).average().orElse(0.0)) * 10.0) / 10.0
                : 0.0;

        Map<String, Long> statusMap = new LinkedHashMap<>();
        for (GoalStatus s : GoalStatus.values()) {
            statusMap.put(s.name(), myGoals.stream().filter(g -> g.getStatus() == s).count());
        }

        return new EmployeeAnalyticsResponse(
                totalGoals,
                totalWeight != null ? totalWeight : BigDecimal.ZERO,
                avgProgress,
                completed,
                inProgress,
                pending,
                activeCycleId,
                cycleName,
                statusMap
        );
    }
}
