package com.practice.springbootdemo.advance_performance_module.dto.search;

import com.practice.springbootdemo.advance_performance_module.entity.CycleStatus;
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
@Schema(description = "Performance Cycle search and pagination parameters")
public class CycleSearchCriteria {
    @Schema(description = "Partial cycle name match", example = "2026 Annual")
    private String name;

    @Schema(description = "Status filter", example = "ACTIVE")
    private CycleStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Earliest start date (YYYY-MM-DD)", example = "2026-01-01")
    private LocalDate startDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Latest end date (YYYY-MM-DD)", example = "2026-12-31")
    private LocalDate endDateTo;

    @Builder.Default
    @Schema(description = "Zero-based page number", example = "0")
    private Integer page = 0;

    @Builder.Default
    @Schema(description = "Page size", example = "10")
    private Integer size = 10;

    @Builder.Default
    @Schema(description = "Sort property", example = "startDate")
    private String sortBy = "startDate";

    @Builder.Default
    @Schema(description = "Sort direction (asc / desc)", example = "desc")
    private String direction = "desc";
}
