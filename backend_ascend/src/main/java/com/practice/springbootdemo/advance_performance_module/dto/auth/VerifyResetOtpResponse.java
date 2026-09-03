package com.practice.springbootdemo.advance_performance_module.dto.auth;

public record VerifyResetOtpResponse(
        String resetAuthorization,
        String message
) {}
