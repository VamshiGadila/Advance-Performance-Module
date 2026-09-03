package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.dto.auth.*;
import com.practice.springbootdemo.advance_performance_module.entity.*;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.exception.ResourceNotFoundException;
import com.practice.springbootdemo.advance_performance_module.exception.UnauthorizedException;
import com.practice.springbootdemo.advance_performance_module.repository.DepartmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.ManagerAssignmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.UserRepository;
import com.practice.springbootdemo.advance_performance_module.security.JwtService;
import com.practice.springbootdemo.advance_performance_module.security.TokenBlacklistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ManagerAssignmentRepository managerAssignmentRepository;
    private final PasswordService passwordService;
    private final PasswordPolicyService passwordPolicyService;
    private final OtpService otpService;
    private final ResetAuthorizationService resetAuthorizationService;
    private final SessionService sessionService;
    private final RateLimitService rateLimitService;
    private final SecurityAuditService securityAuditService;
    private final JwtService jwtService;
    private final UserCodeGeneratorService userCodeGeneratorService;
    private final TokenBlacklistService tokenBlacklistService;
    private final EmailService emailService;

    public AuthService(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            ManagerAssignmentRepository managerAssignmentRepository,
            PasswordService passwordService,
            PasswordPolicyService passwordPolicyService,
            OtpService otpService,
            ResetAuthorizationService resetAuthorizationService,
            SessionService sessionService,
            RateLimitService rateLimitService,
            SecurityAuditService securityAuditService,
            JwtService jwtService,
            UserCodeGeneratorService userCodeGeneratorService,
            TokenBlacklistService tokenBlacklistService,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.managerAssignmentRepository = managerAssignmentRepository;
        this.passwordService = passwordService;
        this.passwordPolicyService = passwordPolicyService;
        this.otpService = otpService;
        this.resetAuthorizationService = resetAuthorizationService;
        this.sessionService = sessionService;
        this.rateLimitService = rateLimitService;
        this.securityAuditService = securityAuditService;
        this.jwtService = jwtService;
        this.userCodeGeneratorService = userCodeGeneratorService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.emailService = emailService;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        log.info("Processing signup request for email: '{}', departmentId: {}", request.email(), request.departmentId());

        if (request.password() == null || !request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        passwordPolicyService.validateNewPassword(request.password(), request.confirmPassword(), null, null);

        String email = request.email() != null ? request.email().trim().toLowerCase() : "";
        if (email.isBlank() || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BadRequestException("Invalid email format: Please provide a valid work email address (e.g. name@company.com)");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.warn("Signup validation failed: Email '{}' already registered", email);
            throw new BadRequestException("Email is already registered: " + email);
        }

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> {
                    log.error("Department ID {} not found during signup", request.departmentId());
                    return new ResourceNotFoundException("Department not found with ID: " + request.departmentId());
                });

        String designation = (request.designation() != null && !request.designation().isBlank())
                ? request.designation().trim()
                : "Software Engineer";

        String employeeCode = userCodeGeneratorService.generateEmployeeCode();
        String hashedPassword = passwordService.encode(request.password());

        User employee = User.builder()
                .employeeCode(employeeCode)
                .name(request.name().trim())
                .email(email)
                .passwordHash(hashedPassword)
                .passwordChangedAt(LocalDateTime.now())
                .failedLoginAttempts(0)
                .role(Role.EMPLOYEE)
                .departmentId(department.getId())
                .designation(designation)
                .status(UserStatus.ACTIVE)
                .active(true)
                .build();

        User saved = userRepository.save(employee);
        passwordPolicyService.recordPasswordInHistory(saved, hashedPassword);
        securityAuditService.recordEvent(saved.getId(), saved.getEmail(), "SIGNUP_SUCCESS", null, "New user registered with code " + saved.getEmployeeCode());

        log.info("Successfully registered Employee: ID={}, Code={}, Email={}", saved.getId(), saved.getEmployeeCode(), saved.getEmail());

        return new SignupResponse(
                saved.getId(),
                saved.getEmployeeCode(),
                saved.getName(),
                saved.getEmail(),
                department.getId(),
                department.getName(),
                "Account created successfully. You can now log in."
        );
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String clientIp, String userAgent) {
        String email = request.email() != null ? request.email().trim() : "";
        log.info("Processing login request for identifier: '{}'", email);

        if (email.contains("@") && !EMAIL_PATTERN.matcher(email.toLowerCase()).matches()) {
            throw new BadRequestException("Invalid email format: Please provide a valid work email address (e.g. name@company.com)");
        }

        rateLimitService.checkLoginRateLimit(clientIp, email);

        User user = userRepository.findByEmailIgnoreCaseOrEmployeeCodeIgnoreCase(email, email)
                .orElseThrow(() -> {
                    log.warn("Authentication failed: User with identifier '{}' not found", email);
                    securityAuditService.recordEvent(null, email, "LOGIN_FAILURE", clientIp, "User not found");
                    return new UnauthorizedException("Invalid username or password.");
                });

        // Check temporary or permanent deactivation with auto-reactivation support
        if (user.getStatus() == UserStatus.DISABLED || user.getDeactivatedUntil() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (user.getDeactivatedUntil() != null && now.isAfter(user.getDeactivatedUntil())) {
                // Auto-reactivation: Suspension period has expired
                log.info("Temporary deactivation period expired for user ID {}. Automatically restoring account to ACTIVE.", user.getId());
                user.setStatus(UserStatus.ACTIVE);
                user.setActive(true);
                user.setDeactivatedUntil(null);
                user.setDeactivationReason(null);
                userRepository.save(user);
                securityAuditService.recordEvent(user.getId(), email, "AUTO_REACTIVATED", clientIp, "Deactivation period expired");
            } else {
                log.warn("Login blocked: Account is deactivated for user ID {}", user.getId());
                securityAuditService.recordEvent(user.getId(), email, "LOGIN_BLOCKED", clientIp, "Account deactivated");

                String timeNotice = "";
                if (user.getDeactivatedUntil() != null) {
                    timeNotice = " until " + user.getDeactivatedUntil().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
                }
                String reasonNotice = (user.getDeactivationReason() != null && !user.getDeactivationReason().isBlank())
                        ? " Reason: " + user.getDeactivationReason() + "."
                        : "";
                throw new UnauthorizedException("Your account has been temporarily deactivated" + timeNotice + "." + reasonNotice + " Please contact HR for assistance.");
            }
        }

        if (!user.isActive()) {
            log.warn("Login blocked: Account is inactive for user ID {}", user.getId());
            securityAuditService.recordEvent(user.getId(), email, "LOGIN_BLOCKED", clientIp, "Account inactive");
            throw new UnauthorizedException("Your account is inactive.");
        }

        if (user.isAccountLocked() || user.getStatus() == UserStatus.LOCKED) {
            log.warn("Login blocked: Account locked until {} for User ID: {}", user.getLockoutUntil(), user.getId());
            securityAuditService.recordEvent(user.getId(), email, "LOGIN_BLOCKED", clientIp, "Account locked");
            throw new UnauthorizedException("Your account is temporarily locked. Please try again later.");
        }

        if (!passwordService.matches(request.password(), user.getPasswordHash())) {
            int currentAttempts = (user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0) + 1;
            user.setFailedLoginAttempts(currentAttempts);

            if (currentAttempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockoutUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
                user.setStatus(UserStatus.LOCKED);
                userRepository.save(user);
                securityAuditService.recordEvent(user.getId(), email, "ACCOUNT_LOCKED", clientIp, "Locked after " + currentAttempts + " failed attempts");
                log.warn("User ID: {} account locked for {} minutes after {} failed attempts", user.getId(), LOCKOUT_MINUTES, currentAttempts);
                throw new UnauthorizedException("Account is temporarily locked due to too many failed attempts. Try again in 15 minutes.");
            }

            userRepository.save(user);
            securityAuditService.recordEvent(user.getId(), email, "LOGIN_FAILURE", clientIp, "Failed password attempt " + currentAttempts);
            int remaining = MAX_FAILED_ATTEMPTS - currentAttempts;
            log.warn("Invalid credentials for user: '{}'. Remaining attempts before lock: {}", email, remaining);
            throw new UnauthorizedException("Invalid username or password.");
        }

        // Reset failed login counter and clear lockout status
        user.setFailedLoginAttempts(0);
        user.setLockoutUntil(null);
        user.setStatus(UserStatus.ACTIVE);

        // Transparent Migration: If password was stored with legacy BCrypt, rehash to Argon2id
        if (passwordService.needsRehash(user.getPasswordHash())) {
            log.info("Transparently migrating password for user ID {} to Argon2id", user.getId());
            String upgradedHash = passwordService.encode(request.password());
            user.setPasswordHash(upgradedHash);
            passwordPolicyService.recordPasswordInHistory(user, upgradedHash);
        }

        userRepository.save(user);

        // Register multi-device active session
        sessionService.createSession(user, clientIp, userAgent, LocalDateTime.now().plusHours(24));

        String token = jwtService.generate(user);
        securityAuditService.recordEvent(user.getId(), email, "LOGIN_SUCCESS", clientIp, "Login successful via LOCAL");

        String departmentName = null;
        if (user.getDepartmentId() != null) {
            departmentName = departmentRepository.findById(user.getDepartmentId())
                    .map(Department::getName)
                    .orElse(null);
        }

        log.info("Login successful for User ID: {}, Code: {}, Role: {}", user.getId(), user.getEmployeeCode(), user.getRole());

        return new LoginResponse(
                token,
                user.getId(),
                user.getEmployeeCode(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getDepartmentId(),
                departmentName
        );
    }

    @Transactional
    public String forgotPassword(ForgotPasswordRequest request, String clientIp) {
        String email = request.email() != null ? request.email().trim().toLowerCase() : "";
        if (email.isBlank() || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BadRequestException("Invalid email format: Please provide a valid work email address (e.g. name@company.com)");
        }
        log.info("Processing forgot password request for email: '{}'", email);

        rateLimitService.checkOtpRateLimit(clientIp, email);

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

        // Per explicit directive: notify clearly if user is not registered rather than ambiguous generic message
        if (user == null || !user.isActive()) {
            log.warn("Forgot password rejected: Non-existent or inactive email '{}'", email);
            securityAuditService.recordEvent(null, email, "OTP_REQUEST_FAILED", clientIp, "User not registered or inactive");
            throw new BadRequestException("No registered account found with this email address.");
        }

        String rawOtp = otpService.generateAndSaveOtp(user, VerificationPurpose.PASSWORD_RESET);
        emailService.sendPasswordResetOtp(user.getEmail(), user.getName(), rawOtp);
        securityAuditService.recordEvent(user.getId(), email, "OTP_SENT", clientIp, "Password reset OTP dispatched");

        return "A verification OTP has been sent to your email.";
    }

    public VerifyResetOtpResponse verifyResetOtp(VerifyOtpRequest request, String clientIp) {
        String email = request.email().trim().toLowerCase();
        String otp = request.otp().trim();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException("No registered account found with this email address."));

        if (!user.isActive()) {
            throw new BadRequestException("Account is inactive. Please contact HR administrator.");
        }

        otpService.verifyOtp(user, VerificationPurpose.PASSWORD_RESET, otp);
        String resetAuthToken = resetAuthorizationService.createResetAuthorization(user);
        securityAuditService.recordEvent(user.getId(), email, "OTP_VERIFICATION_SUCCESS", clientIp, "Reset OTP verified");

        return new VerifyResetOtpResponse(resetAuthToken, "OTP verified successfully. You can now create a new password.");
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request, String clientIp) {
        User user;

        // Support decoupled ResetAuthorization flow as well as legacy email+otp flow
        if (request.resetAuthorization() != null && !request.resetAuthorization().isBlank()) {
            user = resetAuthorizationService.validateAndConsumeAuthorization(request.resetAuthorization());
        } else if (request.email() != null && request.otp() != null) {
            String email = request.email().trim().toLowerCase();
            user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new BadRequestException("No user found matching email '" + email + "'"));
            otpService.verifyOtp(user, VerificationPurpose.PASSWORD_RESET, request.otp().trim());
        } else {
            throw new BadRequestException("Password reset authorization or OTP code is required.");
        }

        if (!user.isActive()) {
            throw new BadRequestException("Account is inactive. Please contact HR administrator.");
        }

        passwordPolicyService.validateNewPassword(request.newPassword(), request.confirmPassword(), user, null);

        String newHashedPassword = passwordService.encode(request.newPassword());
        user.setPasswordHash(newHashedPassword);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setLockoutUntil(null);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        passwordPolicyService.recordPasswordInHistory(user, newHashedPassword);

        // Security Non-Negotiable: Revoke ALL sessions across all devices
        sessionService.revokeAllSessions(user.getId());
        securityAuditService.recordEvent(user.getId(), user.getEmail(), "PASSWORD_RESET_SUCCESS", clientIp, "Password reset; all sessions revoked");

        log.info("Password successfully reset for User ID: {}, Code: {}, Email: {}", user.getId(), user.getEmployeeCode(), user.getEmail());
        return "Password reset successfully. Please log in again with your new password.";
    }

    @Transactional
    public String changePassword(Long userId, ChangePasswordRequest request, String currentSessionId, String clientIp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (!passwordService.matches(request.currentPassword(), user.getPasswordHash())) {
            securityAuditService.recordEvent(userId, user.getEmail(), "PASSWORD_CHANGE_FAILED", clientIp, "Current password incorrect");
            throw new BadRequestException("Current password is incorrect");
        }

        passwordPolicyService.validateNewPassword(request.newPassword(), request.confirmPassword(), user, request.currentPassword());

        String newHashedPassword = passwordService.encode(request.newPassword());
        user.setPasswordHash(newHashedPassword);
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        passwordPolicyService.recordPasswordInHistory(user, newHashedPassword);

        // Security Non-Negotiable: Revoke OTHER sessions
        sessionService.revokeOtherSessions(userId, currentSessionId);
        securityAuditService.recordEvent(userId, user.getEmail(), "PASSWORD_CHANGE_SUCCESS", clientIp, "Password changed; other sessions revoked");

        log.info("Password successfully changed for User ID: {}", user.getId());
        return "Password changed successfully. Other active sessions have been signed out.";
    }

    public void logout(String authHeader, Long userId, String sessionId) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            long expiryTime = jwtService.getExpirationTime(token);
            tokenBlacklistService.blacklistToken(token, expiryTime);
        }

        if (userId != null && sessionId != null) {
            try {
                sessionService.revokeSession(userId, sessionId);
            } catch (Exception ex) {
                log.debug("Session revoke on logout: {}", ex.getMessage());
            }
        }

        securityAuditService.recordEvent(userId, null, "LOGOUT", null, "User signed out");
        log.info("User successfully logged out and token blacklisted.");
    }

    @Transactional
    public void logoutAll(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        sessionService.revokeAllSessions(userId);
        securityAuditService.recordEvent(userId, user.getEmail(), "LOGOUT_ALL", null, "All active sessions revoked");
        log.info("All active sessions revoked for User ID: {}", userId);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getActiveSessions(Long userId, String currentSessionId) {
        return sessionService.getActiveSessions(userId, currentSessionId);
    }

    @Transactional
    public void revokeSession(Long userId, String sessionId) {
        sessionService.revokeSession(userId, sessionId);
        securityAuditService.recordEvent(userId, null, "SESSION_REVOKED", null, "Session " + sessionId + " revoked");
    }

    @Transactional(readOnly = true)
    public LoginResponse getCurrentUser(Long userId) {
        log.debug("Fetching profile details for User ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User profile lookup failed for User ID: {}", userId);
                    return new ResourceNotFoundException("User not found with ID: " + userId);
                });

        if (!user.isActive()) {
            log.warn("Profile retrieval blocked for inactive User ID: {}", userId);
            throw new UnauthorizedException("Your account is inactive");
        }

        String departmentName = null;
        if (user.getDepartmentId() != null) {
            departmentName = departmentRepository.findById(user.getDepartmentId())
                    .map(Department::getName)
                    .orElse(null);
        }

        log.debug("Retrieved profile for User ID: {}, Code: {}", user.getId(), user.getEmployeeCode());
        return new LoginResponse(
                null,
                user.getId(),
                user.getEmployeeCode(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getDepartmentId(),
                departmentName
        );
    }

    // Convenience Overloads for Backward Compatibility and Unit Tests
    @Transactional
    public LoginResponse login(LoginRequest request) {
        return login(request, "127.0.0.1", "TestClient");
    }

    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        return forgotPassword(request, "127.0.0.1");
    }

    @Transactional
    public String verifyOtp(VerifyOtpRequest request) {
        verifyResetOtp(request, "127.0.0.1");
        return "OTP verified successfully. You can now create a new password.";
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        return resetPassword(request, "127.0.0.1");
    }

    @Transactional
    public String changePassword(Long userId, ChangePasswordRequest request) {
        return changePassword(userId, request, null, "127.0.0.1");
    }

    public void logout(String authHeader) {
        logout(authHeader, null, null);
    }
}