package io.reflectoring.digitalwallet.application.port.in;

public interface LoginUseCase {

	LoginResult login(LoginCommand command);

	record LoginCommand(
			String username,
			String rawPassword
	) {
		public LoginCommand {
			if (username == null || username.isBlank()) {
				throw new IllegalArgumentException("Username không được để trống");
			}
			if (rawPassword == null || rawPassword.isBlank()) {
				throw new IllegalArgumentException("Mật khẩu không được để trống");
			}
		}
	}

	record LoginResult(
			Long userId,
			String username,
			Long walletId,
			String token
	) {}
}
