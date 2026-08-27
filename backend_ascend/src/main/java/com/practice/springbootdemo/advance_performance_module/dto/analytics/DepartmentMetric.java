package com.practice.springbootdemo.advance_performance_module.dto.analytics;

public record DepartmentMetric(
        Long departmentId,
        String departmentName,
        long employeeCount,
        double goalCompletionRate
) {}
