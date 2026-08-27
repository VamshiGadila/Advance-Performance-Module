package com.practice.springbootdemo.advance_performance_module.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Work email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Username, Name, or Employee Code is required")
        String username,

        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String newPassword,

        @NotBlank(message = "Confirm password is required")
        String confirmPassword
) {}
