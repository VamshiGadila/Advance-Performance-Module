package com.practice.springbootdemo.advance_performance_module.controller.hr;

import com.practice.springbootdemo.advance_performance_module.dto.common.ApiResponse;
import com.practice.springbootdemo.advance_performance_module.dto.common.PagedResponse;
import com.practice.springbootdemo.advance_performance_module.dto.hr.CreateCycleRequest;
import com.practice.springbootdemo.advance_performance_module.dto.hr.CycleResponse;
import com.practice.springbootdemo.advance_performance_module.dto.hr.UpdateCycleRequest;
import com.practice.springbootdemo.advance_performance_module.dto.search.CycleSearchCriteria;
import com.practice.springbootdemo.advance_performance_module.security.SecurityUtils;
import com.practice.springbootdemo.advance_performance_module.service.hr.PerformanceCycleService;
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
@RequestMapping("/api/hr/performance-cycles")
@Tag(name = "Performance Cycles", description = "HR Performance Cycle Lifecycle APIs")
@SecurityRequirement(name = "BearerAuth")
public class PerformanceCycleController {
    private final PerformanceCycleService service;
    private final SearchService searchService;

    public PerformanceCycleController(PerformanceCycleService service, SearchService searchService) {
        this.service = service;
        this.searchService = searchService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Create Performance Cycle", description = "HR creates a cycle in DRAFT status")
    public ApiResponse<CycleResponse> create(@Valid @RequestBody CreateCycleRequest request) {
        Long hrUserId = SecurityUtils.getCurrentUserId();
        log.info("REST: POST /api/hr/performance-cycles - HR ID {} creating cycle '{}'", hrUserId, request.name());
        CycleResponse response = service.create(request, hrUserId);
        return ApiResponse.success("Performance cycle created successfully", response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get All Performance Cycles")
    public ApiResponse<List<CycleResponse>> getAll() {
        log.debug("REST: GET /api/hr/performance-cycles - Listing all cycles");
        return ApiResponse.success(service.getAll());
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('HR', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Search Performance Cycles", description = "Dynamic multi-filter search with sorting and 0-based pagination")
    public ApiResponse<PagedResponse<CycleResponse>> search(CycleSearchCriteria criteria) {
        log.debug("REST: GET /api/hr/performance-cycles/search - Criteria: {}", criteria);
        return ApiResponse.success(searchService.searchCycles(criteria));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('HR', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get Active Cycle")
    public ApiResponse<CycleResponse> getActive() {
        log.debug("REST: GET /api/hr/performance-cycles/active - Fetching active cycle");
        return ApiResponse.success(service.getActive());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'MANAGER', 'EMPLOYEE')")
    public ApiResponse<CycleResponse> getById(@PathVariable Long id) {
        log.debug("REST: GET /api/hr/performance-cycles/{} - Fetching cycle details", id);
        return ApiResponse.success(service.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public ApiResponse<CycleResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateCycleRequest request) {
        log.info("REST: PUT /api/hr/performance-cycles/{} - Updating cycle details", id);
        return ApiResponse.success("Performance cycle updated successfully", service.update(id, request));
    }

    @PatchMapping("/{id}/launch")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Launch Performance Cycle", description = "Transitions cycle from DRAFT to ACTIVE")
    public ApiResponse<CycleResponse> launch(@PathVariable Long id) {
        log.info("REST: PATCH /api/hr/performance-cycles/{}/launch - Activating cycle", id);
        return ApiResponse.success("Performance cycle launched successfully", service.activate(id));
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Close Performance Cycle", description = "Transitions cycle to CLOSED")
    public ApiResponse<CycleResponse> close(@PathVariable Long id) {
        log.info("REST: PATCH /api/hr/performance-cycles/{}/close - Closing cycle", id);
        return ApiResponse.success("Performance cycle closed successfully", service.close(id));
    }
}
