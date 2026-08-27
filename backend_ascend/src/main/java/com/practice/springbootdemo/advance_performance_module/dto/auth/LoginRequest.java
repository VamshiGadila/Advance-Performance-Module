package com.practice.springbootdemo.advance_performance_module.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


@Schema(description = "User login payload")
public record LoginRequest(
        @NotBlank(message = "Employee ID or Email is required")
        @Schema(example = "EMP001 or hr@ascend.local", description = "Permanent Employee ID (e.g. EMP001) or registered Work Email")
        String email,
        @NotBlank(message = "Password is required")
        @Schema(example = "Password1")
        String password
) {}