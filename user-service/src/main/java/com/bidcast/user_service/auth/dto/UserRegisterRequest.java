package com.bidcast.user_service.auth.dto;

import com.bidcast.user_service.user.UserRole;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

@Schema(
        name = "UserRegisterRequest",
        description = "Required payload to register a new user in Bidcast"
)
public record UserRegisterRequest(

    @Schema(
            description = "User's visible full name",
            example = "John Doe",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Full name is required")
    String fullName,

    @Schema(
            description = "Unique user email within the platform",
            example = "john.doe@bidcast.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    String email,

    @Schema(
            description = "User's initial password. Must be at least 6 characters long",
            example = "Password123!",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    String password,

    @ArraySchema(
            arraySchema = @Schema(
                    description = "List of roles assigned to the user",
                    requiredMode = Schema.RequiredMode.REQUIRED
            ),
            schema = @Schema(
                    implementation = UserRole.class,
                    example = "ADVERTISER"
            )
    )
    @NotEmpty(message = "You must select at least one role")
    Set<UserRole> roles 
) {}
