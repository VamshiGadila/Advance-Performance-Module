package com.practice.springbootdemo.advance_performance_module.util;

import com.practice.springbootdemo.advance_performance_module.entity.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;


public final class TestDataFactory {

    private static final AtomicLong ID_GEN = new AtomicLong(1000);

    private TestDataFactory() {}

    public static long nextId() {
        return ID_GEN.incrementAndGet();
    }

    public static String randomEmail(String prefix) {
        return prefix.toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 6) + "@ascend.local";
    }

    public static Department createDynamicDepartment() {
        return createDynamicDepartment("Dept_" + UUID.randomUUID().toString().substring(0, 5));
    }

    public static Department createDynamicDepartment(String name) {
        Department dept = new Department();
        dept.setId(nextId());
        dept.setName(name);
        dept.setDescription("Dynamic Description for " + name);
        return dept;
    }

    public static User createDynamicUser(Role role) {
        long id = nextId();
        String code = switch (role) {
            case HR -> "HR" + id;
            case MANAGER -> "MGR" + id;
            case EMPLOYEE -> "EMP" + id;
        };
        return User.builder()
                .id(id)
                .employeeCode(code)
                .name("Dynamic User " + id)
                .email(randomEmail(role.name()))
                .passwordHash("$2a$10$dynamicHashedPassword123")
                .role(role)
                .departmentId(1L)
                .skill("Java, Spring, Unit Testing")
                .domain("Engineering")
                .location("Hyderabad")
                .experienceYears((int) (id % 10) + 1)
                .active(true)
                .build();
    }

    public static User createDynamicUser(Long id, String name, String email, Role role, Long deptId) {
        return User.builder()
                .id(id != null ? id : nextId())
                .employeeCode((role == Role.MANAGER ? "MGR" : role == Role.HR ? "HR" : "EMP") + (id != null ? id : nextId()))
                .name(name)
                .email(email)
                .passwordHash("$2a$10$dynamicHashedPassword123")
                .role(role)
                .departmentId(deptId)
                .active(true)
                .build();
    }

    public static PerformanceCycle createDynamicCycle(CycleStatus status) {
        long id = nextId();
        return PerformanceCycle.builder()
                .id(id)
                .name("Dynamic Review Cycle " + id)
                .description("Dynamic Performance Appraisal Cycle")
                .startDate(LocalDate.now().minusMonths(1))
                .endDate(LocalDate.now().plusMonths(5))
                .status(status)
                .createdBy(1L)
                .build();
    }

    public static Goal createDynamicGoal(
            Long employeeId,
            Long managerId,
            Long cycleId,
            GoalType goalType,
            BigDecimal weight,
            GoalStatus status
    ) {
        long id = nextId();
        return Goal.builder()
                .id(id)
                .cycleId(cycleId != null ? cycleId : 1L)
                .employeeId(employeeId != null ? employeeId : nextId())
                .managerId(managerId != null ? managerId : nextId())
                .goalType(goalType != null ? goalType : GoalType.OKR)
                .goalScope(GoalScope.INDIVIDUAL)
                .title("Dynamic Objective " + id)
                .description("Detailed description for dynamic goal " + id)
                .target("Metric benchmark for goal " + id)
                .weight(weight != null ? weight : new BigDecimal("25.00"))
                .dueDate(LocalDate.now().plusMonths(3))
                .status(status != null ? status : GoalStatus.PENDING_ACCEPTANCE)
                .progress(status == GoalStatus.COMPLETED ? 100 : status == GoalStatus.IN_PROGRESS ? 50 : 0)
                .employeeAccepted(status != GoalStatus.PENDING_ACCEPTANCE)
                .modificationRequested(status == GoalStatus.MODIFICATION_REQUESTED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static ManagerAssignment createDynamicAssignment(Long employeeId, Long managerId, Long cycleId) {
        return ManagerAssignment.builder()
                .id(nextId())
                .employeeId(employeeId)
                .managerId(managerId)
                .performanceCycleId(cycleId)
                .active(true)
                .assignedDate(LocalDateTime.now())
                .build();
    }

    public static GoalModificationRequest createDynamicModRequest(
            Long goalId,
            Long employeeId,
            Long managerId,
            ModificationStatus status
    ) {
        return GoalModificationRequest.builder()
                .id(nextId())
                .goalId(goalId)
                .employeeId(employeeId)
                .managerId(managerId)
                .comment("Dynamic modification reasoning for goal " + goalId)
                .requestedChanges("Dynamic timeline adjustment")
                .status(status != null ? status : ModificationStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();
    }
}
