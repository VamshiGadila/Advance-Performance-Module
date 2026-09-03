package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.exception.TooManyRequestsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RateLimitService {

    private static class RequestRecord {
        int count;
        long windowStartEpoch;

        RequestRecord(long windowStartEpoch) {
            this.count = 1;
            this.windowStartEpoch = windowStartEpoch;
        }
    }

    private final Map<String, RequestRecord> rateLimitStore = new ConcurrentHashMap<>();

    // Login: Max 10 attempts per minute per key
    private static final int MAX_LOGIN_ATTEMPTS = 10;
    private static final long LOGIN_WINDOW_SECONDS = 60;

    // Forgot Password OTP: Max 5 requests per 10 minutes per email/IP
    private static final int MAX_OTP_REQUESTS = 5;
    private static final long OTP_WINDOW_SECONDS = 600;

    public void checkLoginRateLimit(String clientIp, String identifier) {
        String key = "login:" + (clientIp != null ? clientIp : "unknown") + ":" + identifier.toLowerCase().trim();
        checkLimit(key, MAX_LOGIN_ATTEMPTS, LOGIN_WINDOW_SECONDS, "Too many login attempts. Please wait before trying again.");
    }

    public void checkOtpRateLimit(String clientIp, String email) {
        String key = "otp:" + (clientIp != null ? clientIp : "unknown") + ":" + email.toLowerCase().trim();
        checkLimit(key, MAX_OTP_REQUESTS, OTP_WINDOW_SECONDS, "Too many password reset requests. Please try again in a few minutes.");
    }

    private void checkLimit(String key, int maxRequests, long windowSeconds, String errorMessage) {
        long now = Instant.now().getEpochSecond();

        rateLimitStore.compute(key, (k, existing) -> {
            if (existing == null || (now - existing.windowStartEpoch) >= windowSeconds) {
                return new RequestRecord(now);
            }

            if (existing.count >= maxRequests) {
                long retryAfter = windowSeconds - (now - existing.windowStartEpoch);
                log.warn("Rate limit triggered for key '{}'. Exceeded {} requests. Retry after {}s", key, maxRequests, retryAfter);
                throw new TooManyRequestsException(errorMessage, Math.max(1, retryAfter));
            }

            existing.count++;
            return existing;
        });
    }

    public void clearLimit(String key) {
        rateLimitStore.remove(key);
    }
}
