package com.practice.springbootdemo.advance_performance_module.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Employee registration payload")
public record SignupRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,
        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters")
        String password,
        @NotBlank(message = "Confirm password is required")
        String confirmPassword,
        @NotNull(message = "Department ID is required")
        Long departmentId,
        String designation
) {
    public SignupRequest(String name, String email, String password, String confirmPassword, Long departmentId) {
        this(name, email, password, confirmPassword, departmentId, null);
    }
}