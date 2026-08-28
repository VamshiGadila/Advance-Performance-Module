package com.practice.springbootdemo.advance_performance_module.dto.hr;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Create Manager Account Request")
public record CreateManagerRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        @Schema(example = "Marcus Vance")
        String name,
        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email")
        @Schema(example = "marcus.vance@ascend.local")
        String email,
        @JsonAlias({"temporaryPassword", "password"})
        @Schema(example = "Password1")
        String password,
        @JsonAlias({"confirmPassword"})
        @Schema(example = "Password1")
        String confirmPassword,
        @NotNull(message = "Department ID is required")
        @Schema(example = "11")
        Long departmentId,
        @Schema(example = "Engineering Manager", description = "Manager designation/title")
        String designation
) {
        public CreateManagerRequest(String name, String email, String password, String confirmPassword, Long departmentId) {
                this(name, email, password, confirmPassword, departmentId, null);
        }

        public String getEffectivePassword() {
                if (password != null && !password.isBlank()) {
                        return password;
                }
                return "Password1";
        }
}