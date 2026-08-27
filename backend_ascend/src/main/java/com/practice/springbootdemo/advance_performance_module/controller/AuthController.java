package com.practice.springbootdemo.advance_performance_module.controller;

import com.practice.springbootdemo.advance_performance_module.dto.auth.*;
import com.practice.springbootdemo.advance_performance_module.dto.common.ApiResponse;
import com.practice.springbootdemo.advance_performance_module.security.SecurityUtils;
import com.practice.springbootdemo.advance_performance_module.service.AuthService;
import com.practice.springbootdemo.advance_performance_module.service.hr.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    @Operation(summary = "Register Employee", description = "Register a new employee account")
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("REST: POST /api/auth/signup - Initiating registration for: {}", request.email());
        SignupResponse response = authService.signup(request);
        log.info("REST: POST /api/auth/signup - Completed registration for ID: {}", response.id());
        return ApiResponse.success("Account registered successfully", response);
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticate credentials and receive JWT Bearer token (Enforces 5-attempt lockout)")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("REST: POST /api/auth/login - Login attempt for: {}", request.email());
        LoginResponse response = authService.login(request);
        log.info("REST: POST /api/auth/login - Authentication successful for User ID: {}", response.id());
        return ApiResponse.success("Login successful", response);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot Password", description = "Generate and send 6-digit password reset OTP")
    public ApiResponse<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("REST: POST /api/auth/forgot-password - Request received for: {}", request.email());
        String result = authService.forgotPassword(request);
        return ApiResponse.success(result, null);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset Password with OTP", description = "Reset account password using 6-digit OTP (Unlocks account and invalidates prior JWTs)")
    public ApiResponse<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("REST: POST /api/auth/reset-password - Reset password attempt for: {}", request.email());
        String result = authService.resetPassword(request);
        return ApiResponse.success(result, null);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change Password", description = "Change password for authenticated user (Invalidates all existing sessions)")
    public ApiResponse<String> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        Long userId = SecurityUtils.getUserIdFromAuth(authentication);
        log.info("REST: POST /api/auth/change-password - Change password for User ID: {}", userId);
        String result = authService.changePassword(userId, request);
        return ApiResponse.success(result, null);
    }

    @PostMapping("/logout")
    @Operation(summary = "User Logout", description = "Blacklist active JWT token on the server")
    public ApiResponse<String> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("REST: POST /api/auth/logout - Processing logout request");
        authService.logout(authHeader);
        return ApiResponse.success("Logged out successfully. Token has been blacklisted.", null);
    }

    @GetMapping("/departments")
    @Operation(summary = "Public Departments List", description = "List departments available for registration")
    public ApiResponse<List<PublicDepartmentResponse>> departments() {
        log.debug("REST: GET /api/auth/departments - Fetching public departments");
        return ApiResponse.success(departmentService.listPublic());
    }

    @GetMapping("/me")
    @Operation(summary = "Get Current User", description = "Retrieve profile details for authenticated user")
    public ApiResponse<LoginResponse> me(Authentication authentication) {
        Long userId = SecurityUtils.getUserIdFromAuth(authentication);
        log.debug("REST: GET /api/auth/me - Fetching profile for User ID: {}", userId);
        return ApiResponse.success(authService.getCurrentUser(userId));
    }
}
