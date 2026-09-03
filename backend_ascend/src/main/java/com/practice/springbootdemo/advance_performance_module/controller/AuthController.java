package com.practice.springbootdemo.advance_performance_module.controller;

import com.practice.springbootdemo.advance_performance_module.dto.auth.*;
import com.practice.springbootdemo.advance_performance_module.dto.common.ApiResponse;
import com.practice.springbootdemo.advance_performance_module.security.SecurityUtils;
import com.practice.springbootdemo.advance_performance_module.service.AuthService;
import com.practice.springbootdemo.advance_performance_module.service.hr.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User Registration, Login, Password Management & Session APIs")
public class AuthController {

    private final AuthService authService;
    private final DepartmentService departmentService;

    public AuthController(AuthService authService, DepartmentService departmentService) {
        this.authService = authService;
        this.departmentService = departmentService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register Employee", description = "Register a new employee account with Argon2id hashing")
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("REST: POST /api/auth/signup - Initiating registration for: {}", request.email());
        SignupResponse response = authService.signup(request);
        log.info("REST: POST /api/auth/signup - Completed registration for ID: {}", response.id());
        return ApiResponse.success("Account registered successfully", response);
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticate credentials, verify lockout, and receive 24h JWT token")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        log.info("REST: POST /api/auth/login - Login attempt for: {} from IP: {}", request.email(), clientIp);

        LoginResponse response = authService.login(request, clientIp, userAgent);
        return ApiResponse.success("Login successful", response);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot Password", description = "Generate and send 6-digit password reset OTP (10m validity, 60s cooldown)")
    public ApiResponse<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        log.info("REST: POST /api/auth/forgot-password - Request for: {} from IP: {}", request.email(), clientIp);

        String result = authService.forgotPassword(request, clientIp);
        return ApiResponse.success(result, null);
    }

    @PostMapping({"/verify-otp", "/verify-reset-otp"})
    @Operation(summary = "Verify Reset OTP", description = "Verify 6-digit OTP code and receive single-use reset authorization token")
    public ApiResponse<VerifyResetOtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request, HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        log.info("REST: POST /api/auth/verify-otp - Verification for: {} from IP: {}", request.email(), clientIp);

        VerifyResetOtpResponse response = authService.verifyResetOtp(request, clientIp);
        return ApiResponse.success(response.message(), response);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset Password", description = "Reset password using reset authorization token; revokes all active sessions")
    public ApiResponse<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        log.info("REST: POST /api/auth/reset-password - Password reset attempt from IP: {}", clientIp);

        String result = authService.resetPassword(request, clientIp);
        return ApiResponse.success(result, null);
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change Password", description = "Change password verifying current password; revokes other device sessions")
    public ApiResponse<String> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        Long userId = SecurityUtils.getUserIdFromAuth(authentication);
        String clientIp = extractClientIp(httpRequest);
        log.info("REST: POST /api/auth/change-password - Change password for User ID: {}", userId);

        String result = authService.changePassword(userId, request, sessionId, clientIp);
        return ApiResponse.success(result, null);
    }

    @PostMapping("/logout")
    @Operation(summary = "User Logout", description = "Blacklist active JWT token and revoke session")
    public ApiResponse<String> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            Authentication authentication
    ) {
        Long userId = authentication != null ? SecurityUtils.getUserIdFromAuth(authentication) : null;
        log.info("REST: POST /api/auth/logout - Processing logout request for User ID: {}", userId);

        authService.logout(authHeader, userId, sessionId);
        return ApiResponse.success("You have been signed out successfully.", null);
    }

    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Logout All Sessions", description = "Revoke all active device sessions for current user")
    public ApiResponse<String> logoutAll(Authentication authentication) {
        Long userId = SecurityUtils.getUserIdFromAuth(authentication);
        log.info("REST: POST /api/auth/logout-all - Revoking all sessions for User ID: {}", userId);

        authService.logoutAll(userId);
        return ApiResponse.success("All active sessions have been signed out.", null);
    }

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get Active Sessions", description = "List all active sessions for current user")
    public ApiResponse<List<SessionResponse>> getActiveSessions(
            @RequestHeader(value = "X-Session-ID", required = false) String currentSessionId,
            Authentication authentication
    ) {
        Long userId = SecurityUtils.getUserIdFromAuth(authentication);
        return ApiResponse.success(authService.getActiveSessions(userId, currentSessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Revoke Specific Session", description = "Sign out a specific device session")
    public ApiResponse<String> revokeSession(@PathVariable String sessionId, Authentication authentication) {
        Long userId = SecurityUtils.getUserIdFromAuth(authentication);
        authService.revokeSession(userId, sessionId);
        return ApiResponse.success("The selected session has been signed out.", null);
    }

    @GetMapping("/departments")
    @Operation(summary = "Public Departments List", description = "List departments available for registration")
    public ApiResponse<List<PublicDepartmentResponse>> departments() {
        log.debug("REST: GET /api/auth/departments - Fetching public departments");
        return ApiResponse.success(departmentService.listPublic());
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get Current User", description = "Retrieve profile details for authenticated user")
    public ApiResponse<LoginResponse> me(Authentication authentication) {
        Long userId = SecurityUtils.getUserIdFromAuth(authentication);
        log.debug("REST: GET /api/auth/me - Fetching profile for User ID: {}", userId);
        return ApiResponse.success(authService.getCurrentUser(userId));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
