package com.practice.springbootdemo.advance_performance_module.dto.hr;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DeactivateEmployeeRequest(
        @NotNull(message = "Duration value is required")
        @Positive(message = "Duration value must be greater than zero")
        Integer durationValue,

        @NotBlank(message = "Duration unit is required")
        @Pattern(regexp = "^(HOURS|DAYS)$", message = "Duration unit must be either HOURS or DAYS")
        String durationUnit,

        @Size(max = 255, message = "Deactivation reason cannot exceed 255 characters")
        String reason
) {}
