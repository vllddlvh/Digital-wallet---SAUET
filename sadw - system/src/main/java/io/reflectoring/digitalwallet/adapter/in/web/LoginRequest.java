package io.reflectoring.digitalwallet.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Yêu cầu đăng nhập tài khoản")
public class LoginRequest {

	@NotBlank(message = "Username không được để trống")
	@Schema(example = "customer_01")
	private String username;

	@NotBlank(message = "Password không được để trống")
	@Schema(example = "Password123!")
	private String password;
}
