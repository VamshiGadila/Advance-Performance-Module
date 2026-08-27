package com.practice.springbootdemo.advance_performance_module.dto.analytics;

import java.util.Map;

public record ManagerAnalyticsResponse(
        long teamSize,
        long totalGoals,
        long completedGoals,
        long inProgressGoals,
        long pendingAcceptanceGoals,
        double teamCompletionRate,
        long pendingModificationRequests,
        Map<String, Long> goalsByStatus
) {}
