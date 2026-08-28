package com.practice.springbootdemo.advance_performance_module.dto.hr;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload to transfer an employee to a new department")
public record TransferDepartmentRequest(
        @NotNull(message = "Department ID is required")
        @Schema(example = "12", description = "Target Department ID")
        Long departmentId
) {}
