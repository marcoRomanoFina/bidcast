package com.bidcast.user_service.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(
        name = "LoginRequest",
        description = "Required payload to authenticate an existing user"
)
public record LoginRequest(
        @Schema(
                description = "Registered user email",
                example = "user@bidcast.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        String email,

        @Schema(
                description = "Plain text password to validate against the stored credential",
                example = "SuperSeguro99",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Password is required")
        String password
) {}
