package com.practice.springbootdemo.advance_performance_module.controller.employee;

import com.practice.springbootdemo.advance_performance_module.dto.common.ApiResponse;
import com.practice.springbootdemo.advance_performance_module.dto.employee.UpdateProfileRequest;
import com.practice.springbootdemo.advance_performance_module.dto.employee.UserProfileResponse;
import com.practice.springbootdemo.advance_performance_module.security.SecurityUtils;
import com.practice.springbootdemo.advance_performance_module.service.hr.EmployeeManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/employee/profile")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Employee Profile", description = "Self-service profile and skill management APIs")
@SecurityRequirement(name = "BearerAuth")
public class EmployeeProfileController {

    private final EmployeeManagementService employeeService;

    public EmployeeProfileController(EmployeeManagementService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    @Operation(summary = "Get My Profile", description = "Retrieve personal profile details, skills, and enterprise credentials")
    public ApiResponse<UserProfileResponse> getMyProfile(Authentication authentication) {
        Long userId = SecurityUtils.getUserIdFromAuth(authentication);
        log.debug("REST: GET /api/employee/profile - User ID: {}", userId);
        return ApiResponse.success(employeeService.getUserProfile(userId));
    }

    @PatchMapping
    @Operation(summary = "Update My Profile", description = "Update professional skills, domain, location, and years of experience")
    public ApiResponse<UserProfileResponse> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        Long userId = SecurityUtils.getUserIdFromAuth(authentication);
        log.info("REST: PATCH /api/employee/profile - Updating profile for User ID: {}", userId);
        UserProfileResponse updated = employeeService.updateUserProfile(userId, request);
        return ApiResponse.success("Profile updated successfully", updated);
    }
}
