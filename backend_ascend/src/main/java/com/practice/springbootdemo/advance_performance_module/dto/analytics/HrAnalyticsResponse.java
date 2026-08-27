package com.practice.springbootdemo.advance_performance_module.dto.analytics;

import java.util.List;
import java.util.Map;

public record HrAnalyticsResponse(
        long totalEmployees,
        long totalManagers,
        long activeAssignments,
        long totalCycles,
        Long activeCycleId,
        String activeCycleName,
        String activeCycleStartDate,
        String activeCycleEndDate,
        long totalGoals,
        long completedGoals,
        double completionRate,
        Map<String, Long> goalsByStatus,
        List<DepartmentMetric> departmentMetrics
) {}
