package com.practice.springbootdemo.advance_performance_module.dto.hr;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload to assign or change an employee's manager")
public record ChangeManagerRequest(
        @NotNull(message = "Manager ID is required")
        @Schema(example = "2", description = "New Manager User ID")
        Long managerId
) {}
