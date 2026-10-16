package sa.uet.digitalwallet_system.adapter.in.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sa.uet.digitalwallet_system.adapter.in.web.dto.request.LoginRequest;
import sa.uet.digitalwallet_system.adapter.in.web.dto.request.RegisterRequest;
import sa.uet.digitalwallet_system.adapter.in.web.dto.response.ApiResponse;
import sa.uet.digitalwallet_system.adapter.in.web.dto.response.AuthResponse;
import sa.uet.digitalwallet_system.adapter.in.web.dto.response.UserResponse;
import sa.uet.digitalwallet_system.application.port.in.LoginCommand;
import sa.uet.digitalwallet_system.application.port.in.LoginUseCase;
import sa.uet.digitalwallet_system.application.port.in.RegisterCommand;
import sa.uet.digitalwallet_system.application.port.in.RegisterUseCase;
import sa.uet.digitalwallet_system.domain.model.User;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration and authentication")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;

    @PostMapping("/register")
    @Operation(summary = "Register a new user account", description = "Registers user and automatically provisions a wallet")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterCommand command = new RegisterCommand(
                request.getUsername(),
                request.getPassword(),
                request.getFullName(),
                request.getEmail()
        );
        User registeredUser = registerUseCase.register(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("User registered successfully", UserResponse.fromDomain(registeredUser)));
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates credentials and returns a Bearer JWT access token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginCommand command = new LoginCommand(request.getUsername(), request.getPassword());
        String token = loginUseCase.login(command);
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .build();
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }
}
