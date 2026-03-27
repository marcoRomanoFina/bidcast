package com.bidcast.user_service.auth;

import com.bidcast.user_service.auth.dto.AuthResponse;
import com.bidcast.user_service.auth.dto.LoginRequest;
import com.bidcast.user_service.auth.dto.UserRegisterRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(
        name = "Authentication",
        description = "Public endpoints for user registration and authentication with JWT issuance"
)
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
            summary = "Register user",
            description = """
                    Creates a new user in the system and returns a signed JWT.

                    Main rules:
                    - email must be unique
                    - password must be at least 6 characters long
                    - at least one role must be provided
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User registered successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(
                                    name = "Successful registration",
                                    value = """
                                            {
                                              "token": "eyJhbGciOiJIUzI1NiJ9..."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid payload",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Validation errors",
                                    value = """
                                            {
                                              "fullName": "Full name is required",
                                              "email": "Email format is invalid",
                                              "password": "Password must be at least 6 characters long",
                                              "roles": "You must select at least one role"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email is already registered",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Duplicate email",
                                    value = """
                                            {
                                              "error": "Email is already registered in Bidcast"
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Log in",
            description = """
                    Validates user credentials and returns a signed JWT.

                    If the email or password do not match, it returns `401 Unauthorized`.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Authentication successful",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(
                                    name = "Successful login",
                                    value = """
                                            {
                                              "token": "eyJhbGciOiJIUzI1NiJ9..."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid payload",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Validation errors",
                                    value = """
                                            {
                                              "email": "Email format is invalid",
                                              "password": "Password is required"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Invalid login",
                                    value = """
                                            {
                                              "error": "Incorrect email or password"
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
