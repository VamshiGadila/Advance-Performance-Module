package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.entity.SecurityAuditLog;
import com.practice.springbootdemo.advance_performance_module.repository.SecurityAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityAuditService {

    private final SecurityAuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEvent(Long userId, String email, String eventType, String clientIp, String details) {
        try {
            SecurityAuditLog logEntry = SecurityAuditLog.builder()
                    .userId(userId)
                    .email(email != null ? email.trim().toLowerCase() : null)
                    .eventType(eventType)
                    .clientIp(clientIp)
                    .details(details)
                    .build();

            auditLogRepository.save(logEntry);
            log.info("SECURITY AUDIT [{}]: User ID={}, Email={}, IP={}, Details={}",
                    eventType, userId, email, clientIp, details);
        } catch (Exception ex) {
            log.warn("Failed to persist security audit log entry: {}", ex.getMessage());
        }
    }
}
