package com.practice.springbootdemo.advance_performance_module.dto.hr;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to assign a Manager to an Employee")
public record AssignManagerRequest(
        @NotNull(message = "Employee ID is required")
        @Schema(description = "Employee User ID", example = "4")
        Long employeeId,
        @NotNull(message = "Manager ID is required")
        @Schema(description = "Manager User ID", example = "2")
        Long managerId,
        @Schema(description = "Optional Performance Cycle ID", example = "1")
        Long performanceCycleId
) {
        public AssignManagerRequest(Long employeeId, Long managerId) {
                this(employeeId, managerId, null);
        }
}