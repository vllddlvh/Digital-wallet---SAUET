package io.reflectoring.digitalwallet.application.port.in;

public interface RegisterUserUseCase {

	Long registerUser(RegisterUserCommand command);

	record RegisterUserCommand(
			String username,
			String rawPassword,
			String fullName,
			String email
	) {
		public RegisterUserCommand {
			if (username == null || username.isBlank()) {
				throw new IllegalArgumentException("Username không được để trống");
			}
			if (rawPassword == null || rawPassword.length() < 6) {
				throw new IllegalArgumentException("Mật khẩu phải từ 6 ký tự trở lên");
			}
			if (email == null || !email.contains("@")) {
				throw new IllegalArgumentException("Email không hợp lệ");
			}
		}
	}
}
