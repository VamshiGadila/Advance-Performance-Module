package com.practice.springbootdemo.advance_performance_module.dto.auth;

import java.time.LocalDateTime;

public record SessionResponse(
        String id,
        String clientIp,
        String userAgent,
        String deviceInfo,
        LocalDateTime lastActiveAt,
        boolean currentSession
) {}
