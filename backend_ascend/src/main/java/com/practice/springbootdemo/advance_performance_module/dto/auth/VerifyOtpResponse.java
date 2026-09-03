package com.practice.springbootdemo.advance_performance_module.dto.auth;

public record VerifyOtpResponse(
        String resetAuthorization,
        String message
) {}
