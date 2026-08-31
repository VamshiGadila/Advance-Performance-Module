package com.practice.springbootdemo.advance_performance_module.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_users_employee_code", columnNames = "employee_code")
        },
        indexes = {
                @Index(name = "idx_user_email", columnList = "email"),
                @Index(name = "idx_user_role", columnList = "role"),
                @Index(name = "idx_user_dept", columnList = "department_id"),
                @Index(name = "idx_user_location", columnList = "location")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_code", nullable = false, unique = true)
    private String employeeCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;


    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", length = 20)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "designation", length = 100)
    private String designation;

    @Column(name = "skill", length = 255)
    private String skill;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "domain", length = 100)
    private String domain;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "lockout_until")
    private LocalDateTime lockoutUntil;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "reset_token", length = 100)
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (passwordChangedAt == null) passwordChangedAt = now;
        if (failedLoginAttempts == null) failedLoginAttempts = 0;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isAccountLocked() {
        return lockoutUntil != null && lockoutUntil.isAfter(LocalDateTime.now());
    }

    public boolean isPasswordExpired() {
        return passwordChangedAt != null && passwordChangedAt.isBefore(LocalDateTime.now().minusDays(90));
    }

    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts != null ? failedLoginAttempts : 0;
    }
}