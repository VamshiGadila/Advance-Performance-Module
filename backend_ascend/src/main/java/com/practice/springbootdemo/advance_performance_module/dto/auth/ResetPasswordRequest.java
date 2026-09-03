package com.practice.springbootdemo.advance_performance_module.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        String resetAuthorization,
        String email,
        String otp,

        @NotBlank(message = "New password is required")
        @Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters")
        String newPassword,

        @NotBlank(message = "Confirm password is required")
        String confirmPassword
) {
    public ResetPasswordRequest(String email, String otp, String newPassword, String confirmPassword) {
        this(null, email, otp, newPassword, confirmPassword);
    }
}
