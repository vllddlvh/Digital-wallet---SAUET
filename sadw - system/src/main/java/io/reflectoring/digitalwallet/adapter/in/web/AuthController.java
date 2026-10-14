package io.reflectoring.digitalwallet.adapter.in.web;

import io.reflectoring.digitalwallet.application.port.in.LoginUseCase;
import io.reflectoring.digitalwallet.application.port.in.LoginUseCase.LoginCommand;
import io.reflectoring.digitalwallet.application.port.in.LoginUseCase.LoginResult;
import io.reflectoring.digitalwallet.application.port.in.RegisterUserUseCase;
import io.reflectoring.digitalwallet.application.port.in.RegisterUserUseCase.RegisterUserCommand;
import io.reflectoring.digitalwallet.common.WebAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@WebAdapter
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Các endpoint xác thực tài khoản và phân quyền người dùng")
public class AuthController {

	private final RegisterUserUseCase registerUserUseCase;
	private final LoginUseCase loginUseCase;

	@Operation(summary = "Đăng ký tài khoản người dùng & tạo ví mới (Public)")
	@PostMapping("/register")
	public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody RegisterRequest request) {
		RegisterUserCommand command = new RegisterUserCommand(
				request.getUsername(),
				request.getPassword(),
				request.getFullName(),
				request.getEmail()
		);

		Long userId = registerUserUseCase.registerUser(command);

		return ResponseEntity.status(HttpStatus.CREATED).body(
				ApiResponse.ok("Đăng ký tài khoản thành công", Map.of(
						"userId", userId,
						"username", request.getUsername()
				))
		);
	}

	@Operation(summary = "Đăng nhập hệ thống & cấp JWT Bearer Token (Public)")
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
		LoginCommand command = new LoginCommand(
				request.getUsername(),
				request.getPassword()
		);

		LoginResult result = loginUseCase.login(command);

		AuthResponse authResponse = AuthResponse.builder()
				.accessToken(result.token())
				.tokenType("Bearer")
				.expiresIn(86400)
				.userId(result.userId())
				.username(result.username())
				.walletId(result.walletId())
				.build();

		return ResponseEntity.ok(ApiResponse.ok("Đăng nhập thành công", authResponse));
	}
}
