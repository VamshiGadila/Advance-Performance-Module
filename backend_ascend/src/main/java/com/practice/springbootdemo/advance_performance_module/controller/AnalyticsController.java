package com.practice.springbootdemo.advance_performance_module.controller;

import com.practice.springbootdemo.advance_performance_module.dto.analytics.EmployeeAnalyticsResponse;
import com.practice.springbootdemo.advance_performance_module.dto.analytics.HrAnalyticsResponse;
import com.practice.springbootdemo.advance_performance_module.dto.analytics.ManagerAnalyticsResponse;
import com.practice.springbootdemo.advance_performance_module.dto.common.ApiResponse;
import com.practice.springbootdemo.advance_performance_module.security.SecurityUtils;
import com.practice.springbootdemo.advance_performance_module.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
@Tag(name = "Analytics", description = "Role-Based Performance & Operational Analytics APIs")
@SecurityRequirement(name = "BearerAuth")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/hr/analytics")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Get HR Executive Analytics", description = "Returns organization-wide performance and operational metrics (HR only)")
    public ApiResponse<HrAnalyticsResponse> getHrAnalytics() {
        log.info("REST: GET /api/hr/analytics - Fetching executive analytics");
        return ApiResponse.success(analyticsService.getHrAnalytics());
    }

    @GetMapping("/manager/analytics")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Get Manager Team Analytics", description = "Returns team-level performance metrics for authenticated manager")
    public ApiResponse<ManagerAnalyticsResponse> getManagerAnalytics() {
        Long managerId = SecurityUtils.getCurrentUserId();
        log.info("REST: GET /api/manager/analytics - Manager ID {} fetching team analytics", managerId);
        return ApiResponse.success(analyticsService.getManagerAnalytics(managerId));
    }

    @GetMapping("/employee/analytics")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get Employee Personal Analytics", description = "Returns personal performance progress metrics for authenticated employee")
    public ApiResponse<EmployeeAnalyticsResponse> getEmployeeAnalytics() {
        Long employeeId = SecurityUtils.getCurrentUserId();
        log.info("REST: GET /api/employee/analytics - Employee ID {} fetching personal analytics", employeeId);
        return ApiResponse.success(analyticsService.getEmployeeAnalytics(employeeId));
    }
}
