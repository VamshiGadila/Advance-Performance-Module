package com.practice.springbootdemo.advance_performance_module.dto.employee;

import com.practice.springbootdemo.advance_performance_module.entity.GoalScope;
import com.practice.springbootdemo.advance_performance_module.entity.GoalStatus;
import com.practice.springbootdemo.advance_performance_module.entity.GoalType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Employee Goal Response")
public record EmployeeGoalResponse(
        Long id,
        Long cycleId,
        GoalType goalType,
        GoalScope goalScope,
        String title,
        String description,
        String target,
        BigDecimal weight,
        LocalDate dueDate,
        GoalStatus status,
        Integer progress,
        String employeeComment,
        String managerComment,
        boolean employeeAccepted,
        boolean modificationRequested,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {}
