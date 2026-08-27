package com.practice.springbootdemo.advance_performance_module.dto.analytics;

import java.math.BigDecimal;
import java.util.Map;

public record EmployeeAnalyticsResponse(
        long totalGoals,
        BigDecimal totalWeightAllocated,
        double averageProgress,
        long completedGoals,
        long inProgressGoals,
        long pendingAcceptanceGoals,
        Long activeCycleId,
        String activeCycleName,
        Map<String, Long> goalsByStatus
) {}
