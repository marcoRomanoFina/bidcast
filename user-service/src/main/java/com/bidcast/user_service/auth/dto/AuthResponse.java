package com.bidcast.user_service.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "AuthResponse",
        description = "Successful authentication or registration response containing the issued JWT"
)
public record AuthResponse(
        @Schema(
                description = "Signed JWT that identifies the authenticated user and exposes their roles",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c3VhcmlvQGJpZGNhc3QuY29tIiwicm9sZXMiOlsiUk9MRV9BRFZFUlRJU0VSIl19.signature"
        )
        String token
) {
}
