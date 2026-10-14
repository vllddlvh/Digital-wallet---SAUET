package io.reflectoring.digitalwallet.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Yêu cầu đăng ký tài khoản người dùng mới")
public class RegisterRequest {

	@NotBlank(message = "Username không được để trống")
	@Size(min = 3, max = 50, message = "Username phải từ 3 đến 50 ký tự")
	@Schema(example = "customer_01")
	private String username;

	@NotBlank(message = "Password không được để trống")
	@Size(min = 6, max = 100, message = "Password phải từ 6 ký tự trở lên")
	@Schema(example = "Password123!")
	private String password;

	@NotBlank(message = "Họ tên không được để trống")
	@Schema(example = "Nguyen Van A")
	private String fullName;

	@NotBlank(message = "Email không được để trống")
	@Email(message = "Email không hợp lệ")
	@Schema(example = "user1@digitalwallet.io")
	private String email;
}
