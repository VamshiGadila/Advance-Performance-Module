package com.practice.springbootdemo.advance_performance_module.dto.hr;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update an existing Manager Assignment")
public record UpdateAssignmentRequest(
        @NotNull(message = "Manager ID is required")
        @Schema(description = "Updated Manager User ID", example = "3")
        Long managerId,
        @Schema(description = "Active status of assignment", example = "true")
        Boolean active,
        @Schema(description = "Performance Cycle ID", example = "1")
        Long performanceCycleId
) {}
