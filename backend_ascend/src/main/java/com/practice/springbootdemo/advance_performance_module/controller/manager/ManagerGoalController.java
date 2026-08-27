package com.practice.springbootdemo.advance_performance_module.controller.manager;

import com.practice.springbootdemo.advance_performance_module.dto.common.ApiResponse;
import com.practice.springbootdemo.advance_performance_module.dto.common.PagedResponse;
import com.practice.springbootdemo.advance_performance_module.dto.hr.EmployeeResponse;
import com.practice.springbootdemo.advance_performance_module.dto.manager.CreateGoalRequest;
import com.practice.springbootdemo.advance_performance_module.dto.manager.GoalResponse;
import com.practice.springbootdemo.advance_performance_module.dto.manager.UpdateGoalRequest;
import com.practice.springbootdemo.advance_performance_module.dto.search.GoalSearchCriteria;
import com.practice.springbootdemo.advance_performance_module.security.SecurityUtils;
import com.practice.springbootdemo.advance_performance_module.service.manager.ManagerGoalService;
import com.practice.springbootdemo.advance_performance_module.service.search.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/manager/goals")
@PreAuthorize("hasRole('MANAGER')")
@Tag(name = "Manager Goals", description = "Manager Goal Allocation & Management APIs")
@SecurityRequirement(name = "BearerAuth")
public class ManagerGoalController {
    private final ManagerGoalService managerGoalService;
    private final SearchService searchService;

    public ManagerGoalController(ManagerGoalService managerGoalService, SearchService searchService) {
        this.managerGoalService = managerGoalService;
        this.searchService = searchService;
    }

    @GetMapping("/team")
    @Operation(summary = "Get Direct Reports", description = "Retrieve all active direct reports assigned to the logged-in manager")
    public ApiResponse<List<EmployeeResponse>> getMyTeam() {
        Long managerId = SecurityUtils.getCurrentUserId();
          log.debug("REST: GET /api/manager/goals/team - Manager ID: {}", managerId);
        return ApiResponse.success(managerGoalService.getMyTeam(managerId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Assign Goal to Employee")
    public ApiResponse<GoalResponse> createGoal(@Valid @RequestBody CreateGoalRequest request) {
        Long managerId = SecurityUtils.getCurrentUserId();
        log.info("REST: POST /api/manager/goals - Manager ID {} assigning goal to Employee ID {}", managerId, request.employeeId());
        return ApiResponse.success("Goal assigned to employee successfully", managerGoalService.create(request, managerId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Goal")
    public ApiResponse<GoalResponse> updateGoal(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGoalRequest request
    ) {
        Long managerId = SecurityUtils.getCurrentUserId();
        log.info("REST: PUT /api/manager/goals/{} - Manager ID {} updating goal", id, managerId);
        return ApiResponse.success("Goal updated successfully", managerGoalService.update(id, request, managerId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete Goal")
    public void deleteGoal(@PathVariable Long id) {
        Long managerId = SecurityUtils.getCurrentUserId();
        log.info("REST: DELETE /api/manager/goals/{} - Manager ID {} deleting goal", id, managerId);
        managerGoalService.delete(id, managerId);
    }

    @GetMapping("/{id}")
    public ApiResponse<GoalResponse> getGoal(@PathVariable Long id) {
        Long managerId = SecurityUtils.getCurrentUserId();
        log.debug("REST: GET /api/manager/goals/{} - Manager ID: {}", id, managerId);
        return ApiResponse.success(managerGoalService.getGoal(id, managerId));
    }

    @GetMapping
    @Operation(summary = "Get All Managed Goals")
    public ApiResponse<List<GoalResponse>> getAllManagerGoals() {
        Long managerId = SecurityUtils.getCurrentUserId();
        log.debug("REST: GET /api/manager/goals - Manager ID: {}", managerId);
        return ApiResponse.success(managerGoalService.getAllManagerGoals(managerId));
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get Employee Goals for Cycle")
    public ApiResponse<List<GoalResponse>> getEmployeeGoals(
            @PathVariable Long employeeId,
            @RequestParam Long cycleId
    ) {
        Long managerId = SecurityUtils.getCurrentUserId();
        log.debug("REST: GET /api/manager/goals/employee/{} - Manager ID: {}, Cycle ID: {}", employeeId, managerId, cycleId);
        return ApiResponse.success(managerGoalService.getEmployeeGoals(employeeId, cycleId, managerId));
    }

    @GetMapping("/search")
    @Operation(summary = "Search Managed Goals", description = "Dynamic search, filter, sort, and pagination on managed goals")
    public ApiResponse<PagedResponse<GoalResponse>> searchManagedGoals(GoalSearchCriteria criteria) {
        Long managerId = SecurityUtils.getCurrentUserId();
        log.debug("REST: GET /api/manager/goals/search - Manager ID: {}", managerId);
        return ApiResponse.success(searchService.searchManagerGoals(criteria, managerId));
    }
}
