package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.dto.auth.*;
import com.practice.springbootdemo.advance_performance_module.entity.Department;
import com.practice.springbootdemo.advance_performance_module.entity.ManagerAssignment;
import com.practice.springbootdemo.advance_performance_module.entity.Role;
import com.practice.springbootdemo.advance_performance_module.entity.User;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.exception.ResourceNotFoundException;
import com.practice.springbootdemo.advance_performance_module.exception.UnauthorizedException;
import com.practice.springbootdemo.advance_performance_module.repository.DepartmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.ManagerAssignmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.UserRepository;
import com.practice.springbootdemo.advance_performance_module.security.JwtService;
import com.practice.springbootdemo.advance_performance_module.security.TokenBlacklistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class AuthService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ManagerAssignmentRepository managerAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserCodeGeneratorService userCodeGeneratorService;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthService(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            ManagerAssignmentRepository managerAssignmentRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserCodeGeneratorService userCodeGeneratorService,
            TokenBlacklistService tokenBlacklistService
    ) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.managerAssignmentRepository = managerAssignmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userCodeGeneratorService = userCodeGeneratorService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        log.info("Processing signup request for email: '{}', departmentId: {}", request.email(), request.departmentId());

        if (!request.password().equals(request.confirmPassword())) {
            log.warn("Signup validation failed: Password mismatch for email '{}'", request.email());
            throw new BadRequestException("Passwords do not match");
        }

        String email = request.email().trim().toLowerCase();
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
        User employee = User.builder()
                .employeeCode(employeeCode)
                .name(request.name().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .passwordChangedAt(LocalDateTime.now())
                .failedLoginAttempts(0)
                .role(Role.EMPLOYEE)
                .departmentId(department.getId())
                .designation(designation)
                .active(true)
                .build();

        User saved = userRepository.save(employee);
        log.info("Successfully registered Employee: ID={}, Code={}, Email={}", saved.getId(), saved.getEmployeeCode(), saved.getEmail());

        if (department.getDefaultManagerId() != null) {
            User manager = userRepository.findById(department.getDefaultManagerId()).orElse(null);
            if (manager != null && manager.getRole() == Role.MANAGER && manager.isActive()) {
                ManagerAssignment assignment = ManagerAssignment.builder()
                        .employeeId(saved.getId())
                        .managerId(manager.getId())
                        .active(true)
                        .build();
                managerAssignmentRepository.save(assignment);
                log.info("Auto-assigned Employee ID {} to Department Default Manager ID {}", saved.getId(), manager.getId());
            }
        }

        return new SignupResponse(
                saved.getId(),
                saved.getEmployeeCode(),
                saved.getName(),
                saved.getEmail(),
                department.getId(),
                saved.getRole().name(),
                "Employee account created successfully"
        );
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public LoginResponse login(LoginRequest request) {
        String identifier = request.email().trim();
        log.info("Processing login request for identifier: '{}'", identifier);

        Optional<User> userOpt = identifier.contains("@")
                ? userRepository.findByEmailIgnoreCase(identifier)
                : userRepository.findByEmployeeCode(identifier);

        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmailIgnoreCaseOrEmployeeCodeIgnoreCase(identifier, identifier);
        }

        User user = userOpt.orElseThrow(() -> {
            log.warn("Login failed: User not found for identifier '{}'", identifier);
            return new UnauthorizedException("Invalid credentials or user not found");
        });

        if (!user.isActive()) {
            log.warn("Login blocked: Inactive account for User ID {}", user.getId());
            throw new UnauthorizedException("Your account is inactive");
        }

        // Scenario 6: Check if account is temporarily locked
        if (user.isAccountLocked()) {
            log.warn("Login blocked: Account temporarily locked for User ID {} until {}", user.getId(), user.getLockoutUntil());
            throw new UnauthorizedException("Account is temporarily locked due to repeated failed login attempts. Please try again after " + user.getLockoutUntil());
        }


        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            int currentAttempts = (user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0) + 1;
            user.setFailedLoginAttempts(currentAttempts);

            if (currentAttempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockoutUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
                userRepository.save(user);
                log.warn("Account locked: User ID {} exceeded max failed attempts ({})", user.getId(), currentAttempts);
                throw new UnauthorizedException("Account has been locked for " + LOCKOUT_MINUTES + " minutes due to " + MAX_FAILED_ATTEMPTS + " consecutive failed login attempts. Please try again after " + user.getLockoutUntil());
            } else {
                userRepository.save(user);
                int attemptsRemaining = MAX_FAILED_ATTEMPTS - currentAttempts;
                log.warn("Login failed: Invalid credentials for User ID {}. Attempts remaining: {}", user.getId(), attemptsRemaining);
                throw new UnauthorizedException("Invalid email or password. " + attemptsRemaining + " attempt(s) remaining before account lockout.");
            }
        }

        if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setLockoutUntil(null);
            userRepository.save(user);
        }

        String token = jwtService.generate(user);
        log.info("Login successful: User ID={}, Code={}, Role={}, Email={}", user.getId(), user.getEmployeeCode(), user.getRole(), user.getEmail());

        String departmentName = null;
        if (user.getDepartmentId() != null) {
            departmentName = departmentRepository.findById(user.getDepartmentId())
                    .map(Department::getName)
                    .orElse(null);
        }

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
    public String forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase();
        log.info("Processing forgot password request for email: '{}'", email);

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || !user.isActive()) {
            log.warn("Forgot password requested for non-existent or inactive email '{}'", email);
            // Return generic message for security
            return "If an active account exists for " + email + ", a 6-digit reset OTP has been generated.";
        }

        // Generate 6-digit cryptographic OTP
        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        user.setResetToken(otp);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        log.info("Password reset OTP generated for User ID {}: OTP='{}' (Expires in 15 minutes)", user.getId(), otp);
        return "Password reset OTP sent successfully! [DEMO OTP: " + otp + "]";
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        String email = request.email().trim().toLowerCase();
        String username = request.username().trim();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException("No user found matching email '" + email + "'"));

        // Match username against employeeCode, name, or email prefix
        boolean matchesCode = user.getEmployeeCode() != null && user.getEmployeeCode().equalsIgnoreCase(username);
        boolean matchesName = user.getName() != null && user.getName().equalsIgnoreCase(username);
        boolean matchesEmailPrefix = email.split("@")[0].equalsIgnoreCase(username);

        if (!matchesCode && !matchesName && !matchesEmailPrefix) {
            log.warn("Reset password verification failed: Username '{}' does not match user '{}' / '{}'",
                    username, user.getName(), user.getEmployeeCode());
            throw new BadRequestException("Username or Employee Code does not match our records for this email.");
        }

        if (!user.isActive()) {
            throw new BadRequestException("Account is inactive. Please contact HR administrator.");
        }

        // Update password and audit fields (Scenario 13: Invalidates previous JWTs)
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setLockoutUntil(null);
        userRepository.save(user);

        log.info("Password successfully reset for User ID: {}, Code: {}, Email: {}", user.getId(), user.getEmployeeCode(), user.getEmail());
        return "Password has been successfully updated! You can now log in with your new credentials.";
    }

    @Transactional
    public String changePassword(Long userId, ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("New passwords do not match");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(LocalDateTime.now()); // Scenario 13
        userRepository.save(user);

        log.info("Password successfully changed for User ID: {}", user.getId());
        return "Password updated successfully. All previous sessions have been invalidated.";
    }

    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            long expiryTime = jwtService.getExpirationTime(token);
            tokenBlacklistService.blacklistToken(token, expiryTime);
            log.info("User successfully logged out and token blacklisted.");
        }
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
}