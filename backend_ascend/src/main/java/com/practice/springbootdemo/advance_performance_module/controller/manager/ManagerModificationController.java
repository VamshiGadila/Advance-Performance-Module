package com.practice.springbootdemo.advance_performance_module.controller.manager;

import com.practice.springbootdemo.advance_performance_module.dto.common.ApiResponse;
import com.practice.springbootdemo.advance_performance_module.dto.goals.GoalModificationResponse;
import com.practice.springbootdemo.advance_performance_module.dto.goals.ModificationReviewRequest;
import com.practice.springbootdemo.advance_performance_module.entity.ModificationStatus;
import com.practice.springbootdemo.advance_performance_module.security.SecurityUtils;
import com.practice.springbootdemo.advance_performance_module.service.goals.GoalModificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/manager/goal-modification-requests")
@PreAuthorize("hasRole('MANAGER')")
@Tag(name = "Manager Goal Modification Review", description = "Manager Modification Review & Decision APIs")
@SecurityRequirement(name = "BearerAuth")
public class ManagerModificationController {
    private final GoalModificationService modificationService;

    public ManagerModificationController(GoalModificationService modificationService) {
        this.modificationService = modificationService;
    }

    @GetMapping
    @Operation(summary = "Get Pending Modification Requests")
    public ApiResponse<List<GoalModificationResponse>> getRequests(@RequestParam(required = false) ModificationStatus status) {
        Long managerId = SecurityUtils.getCurrentUserId();
        log.debug("REST: GET /api/manager/goal-modification-requests - Manager ID: {}, status: {}", managerId, status);
        return ApiResponse.success(modificationService.getManagerModificationRequests(managerId, status));
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve Goal Modification Request")
    public ApiResponse<GoalModificationResponse> approve(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ModificationReviewRequest reviewRequest
    ) {
        Long managerId = SecurityUtils.getCurrentUserId();
        log.info("REST: PATCH /api/manager/goal-modification-requests/{}/approve - Manager ID: {}", id, managerId);
        return ApiResponse.success("Goal modification request approved successfully", modificationService.approveRequest(id, managerId, reviewRequest));
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Reject Goal Modification Request")
    public ApiResponse<GoalModificationResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ModificationReviewRequest reviewRequest
    ) {
        Long managerId = SecurityUtils.getCurrentUserId();
        log.info("REST: PATCH /api/manager/goal-modification-requests/{}/reject - Manager ID: {}", id, managerId);
        return ApiResponse.success("Goal modification request rejected", modificationService.rejectRequest(id, managerId, reviewRequest));
    }
}
