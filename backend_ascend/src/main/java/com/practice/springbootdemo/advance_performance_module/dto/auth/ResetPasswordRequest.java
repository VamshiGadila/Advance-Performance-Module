package com.practice.springbootdemo.advance_performance_module.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Work email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "6-digit OTP code is required")
        @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
        String otp,

        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String newPassword,

        @NotBlank(message = "Confirm password is required")
        String confirmPassword
) {}
