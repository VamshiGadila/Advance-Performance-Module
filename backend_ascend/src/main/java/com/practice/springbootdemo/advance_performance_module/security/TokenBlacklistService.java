package com.practice.springbootdemo.advance_performance_module.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TokenBlacklistService {


    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public void blacklistToken(String token, long expirationMillis) {
        if (token != null && !token.isBlank()) {
            blacklist.put(token, expirationMillis);
            log.info("JWT Token added to blacklist. Current blacklisted tokens count: {}", blacklist.size());
        }
    }

    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        Long expiration = blacklist.get(token);
        if (expiration == null) {
            return false;
        }

        if (System.currentTimeMillis() > expiration) {
            blacklist.remove(token);
            return false;
        }

        return true;
    }
}
