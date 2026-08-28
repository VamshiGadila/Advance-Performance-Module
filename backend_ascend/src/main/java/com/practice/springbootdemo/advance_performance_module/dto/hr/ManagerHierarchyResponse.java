package com.practice.springbootdemo.advance_performance_module.dto.hr;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Manager node with direct report employees in hierarchy tree")
public record ManagerHierarchyResponse(
        Long managerId,
        String managerCode,
        String managerName,
        String managerEmail,
        String managerDesignation,
        Long departmentId,
        String departmentName,
        int totalReports,
        List<EmployeeResponse> directReports
) {}
