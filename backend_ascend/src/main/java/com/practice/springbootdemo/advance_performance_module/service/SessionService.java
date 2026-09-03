package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.dto.auth.SessionResponse;
import com.practice.springbootdemo.advance_performance_module.entity.User;
import com.practice.springbootdemo.advance_performance_module.entity.UserSession;
import com.practice.springbootdemo.advance_performance_module.exception.ResourceNotFoundException;
import com.practice.springbootdemo.advance_performance_module.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserSessionRepository userSessionRepository;

    @Transactional
    public UserSession createSession(User user, String clientIp, String userAgent, LocalDateTime expiresAt) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        String deviceInfo = parseDeviceInfo(userAgent);

        UserSession session = UserSession.builder()
                .id(sessionId)
                .user(user)
                .clientIp(clientIp)
                .userAgent(userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent)
                .deviceInfo(deviceInfo)
                .expiresAt(expiresAt)
                .build();

        userSessionRepository.save(session);
        log.info("Registered active session {} for user ID {} from {}", sessionId, user.getId(), deviceInfo);
        return session;
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getActiveSessions(Long userId, String currentSessionId) {
        LocalDateTime now = LocalDateTime.now();
        List<UserSession> sessions = userSessionRepository.findActiveSessionsByUserId(userId, now);

        return sessions.stream()
                .map(s -> new SessionResponse(
                        s.getId(),
                        s.getClientIp(),
                        s.getUserAgent(),
                        s.getDeviceInfo(),
                        s.getLastActiveAt(),
                        currentSessionId != null && currentSessionId.equals(s.getId())
                ))
                .toList();
    }

    @Transactional
    public void revokeSession(Long userId, String sessionId) {
        UserSession session = userSessionRepository.findByIdAndRevokedAtIsNull(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Active session not found with ID: " + sessionId));

        if (!session.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Active session not found with ID: " + sessionId);
        }

        session.setRevokedAt(LocalDateTime.now());
        userSessionRepository.save(session);
        log.info("Session {} revoked for user ID {}", sessionId, userId);
    }

    @Transactional
    public void revokeAllSessions(Long userId) {
        int count = userSessionRepository.revokeAllByUserId(userId, LocalDateTime.now());
        log.info("Revoked all active sessions ({}) for user ID {}", count, userId);
    }

    @Transactional
    public void revokeOtherSessions(Long userId, String currentSessionId) {
        if (currentSessionId == null || currentSessionId.isBlank()) {
            revokeAllSessions(userId);
            return;
        }
        int count = userSessionRepository.revokeAllByUserIdExcept(userId, currentSessionId, LocalDateTime.now());
        log.info("Revoked other active sessions ({}) for user ID {}", count, userId);
    }

    private String parseDeviceInfo(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "Unknown Device";
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return "Mobile Device";
        }
        if (ua.contains("edg/")) return "Edge Browser";
        if (ua.contains("chrome/")) return "Chrome Browser";
        if (ua.contains("firefox/")) return "Firefox Browser";
        if (ua.contains("safari/")) return "Safari Browser";
        return "Web Browser";
    }
}
