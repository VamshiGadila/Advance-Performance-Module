package com.practice.springbootdemo.advance_performance_module.dto.employee;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Employee personal profile update payload")
public record UpdateProfileRequest(
        @NotBlank(message = "Skills cannot be blank")
        @Size(min = 2, max = 255, message = "Skills must be between 2 and 255 characters")
        @Schema(example = "Java, Spring Boot, PostgreSQL, Docker", description = "Core technical skills")
        String skill,

        @NotBlank(message = "Domain cannot be blank")
        @Size(min = 2, max = 100, message = "Domain must be between 2 and 100 characters")
        @Schema(example = "Backend & Cloud Architecture", description = "Functional domain")
        String domain,

        @NotBlank(message = "Location cannot be blank")
        @Size(min = 2, max = 100, message = "Location must be between 2 and 100 characters")
        @Schema(example = "Hyderabad, India", description = "Base office location")
        String location,

        @NotNull(message = "Years of experience is required")
        @Min(value = 0, message = "Experience years cannot be negative")
        @Max(value = 50, message = "Experience years cannot exceed 50")
        @Schema(example = "4", description = "Total years of professional experience")
        Integer experienceYears
) {}
