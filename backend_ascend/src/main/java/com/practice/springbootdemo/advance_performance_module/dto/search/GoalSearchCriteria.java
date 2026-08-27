package com.practice.springbootdemo.advance_performance_module.dto.search;

import com.practice.springbootdemo.advance_performance_module.entity.GoalStatus;
import com.practice.springbootdemo.advance_performance_module.entity.GoalType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Goal dynamic search and pagination parameters")
public class GoalSearchCriteria {
    @Schema(description = "Employee ID filter", example = "25")
    private Long employeeId;
    @Schema(description = "Manager ID filter", example = "10")
    private Long managerId;
    @Schema(description = "Performance Cycle ID filter", example = "1")
    private Long performanceCycleId;
    @Schema(description = "Goal status filter", example = "IN_PROGRESS")
    private GoalStatus status;
    @Schema(description = "Goal type filter", example = "OKR")
    private GoalType goalType;
    @Schema(description = "Partial title match", example = "API Performance")
    private String title;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Earliest due date (YYYY-MM-DD)", example = "2026-01-01")
    private LocalDate dueDateFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Latest due date (YYYY-MM-DD)", example = "2026-12-31")
    private LocalDate dueDateTo;
    @Schema(description = "Minimum progress percentage", example = "0")
    private Integer minProgress;
    @Schema(description = "Maximum progress percentage", example = "100")
    private Integer maxProgress;
    @Builder.Default
    @Schema(description = "Zero-based page number", example = "0")
    private Integer page = 0;
    @Builder.Default
    @Schema(description = "Page size", example = "10")
    private Integer size = 10;
    @Builder.Default
    @Schema(description = "Sort property", example = "createdAt")
    private String sortBy = "createdAt";
    @Builder.Default
    @Schema(description = "Sort direction (asc / desc)", example = "desc")
    private String direction = "desc";
}