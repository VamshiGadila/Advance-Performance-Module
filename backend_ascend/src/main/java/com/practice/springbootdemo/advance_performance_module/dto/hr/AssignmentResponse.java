package com.practice.springbootdemo.advance_performance_module.dto.hr;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Manager to Employee assignment details response")
public record AssignmentResponse(
        @Schema(description = "Assignment ID", example = "1")
        Long id,
        @Schema(description = "Employee User ID", example = "4")
        Long employeeId,
        @Schema(description = "Employee Name", example = "John Doe")
        String employeeName,
        @Schema(description = "Employee Code", example = "EMP001")
        String employeeCode,
        @Schema(description = "Manager User ID", example = "2")
        Long managerId,
        @Schema(description = "Manager Name", example = "Alice Smith")
        String managerName,
        @Schema(description = "Manager Code", example = "MGR001")
        String managerCode,
        @Schema(description = "Performance Cycle ID", example = "1")
        Long performanceCycleId,
        @Schema(description = "Performance Cycle Name", example = "2026 Annual Review")
        String cycleName,
        @Schema(description = "Active status", example = "true")
        boolean active,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "Assignment timestamp", example = "2026-01-15T09:00:00")
        LocalDateTime assignedDate
) {
    public AssignmentResponse(
            Long id,
            Long employeeId,
            String employeeName,
            String employeeCode,
            Long managerId,
            String managerName,
            String managerCode,
            boolean active
    ) {
        this(id, employeeId, employeeName, employeeCode, managerId, managerName, managerCode, null, null, active, null);
    }
}
