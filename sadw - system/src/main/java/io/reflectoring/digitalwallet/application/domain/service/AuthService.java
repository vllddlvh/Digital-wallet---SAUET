package io.reflectoring.digitalwallet.application.domain.service;

import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;
import io.reflectoring.digitalwallet.application.domain.model.User;
import io.reflectoring.digitalwallet.application.domain.model.User.UserId;
import io.reflectoring.digitalwallet.application.port.in.LoginUseCase;
import io.reflectoring.digitalwallet.application.port.in.RegisterUserUseCase;
import io.reflectoring.digitalwallet.application.port.out.CreateAccountPort;
import io.reflectoring.digitalwallet.application.port.out.CreateUserPort;
import io.reflectoring.digitalwallet.application.port.out.LoadUserPort;
import io.reflectoring.digitalwallet.application.port.out.PasswordEncoderPort;
import io.reflectoring.digitalwallet.application.port.out.TokenGeneratorPort;
import io.reflectoring.digitalwallet.common.UseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
@Transactional
public class AuthService implements RegisterUserUseCase, LoginUseCase {

	private final LoadUserPort loadUserPort;
	private final CreateUserPort createUserPort;
	private final CreateAccountPort createAccountPort;
	private final PasswordEncoderPort passwordEncoderPort;
	private final TokenGeneratorPort tokenGeneratorPort;

	@Override
	public Long registerUser(RegisterUserCommand command) {
		if (loadUserPort.existsByUsername(command.username())) {
			throw new IllegalArgumentException("Tên đăng nhập '" + command.username() + "' đã tồn tại");
		}

		String encodedPassword = passwordEncoderPort.encode(command.rawPassword());

		User newUser = new User(
				null,
				command.username(),
				encodedPassword,
				command.fullName(),
				command.email(),
				"ROLE_USER"
		);

		User savedUser = createUserPort.createUser(newUser);
		AccountId newAccount = createAccountPort.createAccount(savedUser.getId().getValue());

		return savedUser.getId().getValue();
	}

	@Override
	public LoginResult login(LoginCommand command) {
		User user = loadUserPort.loadUserByUsername(command.username())
				.orElseThrow(() -> new IllegalArgumentException("Tên đăng nhập hoặc mật khẩu không chính xác"));

		if (!passwordEncoderPort.matches(command.rawPassword(), user.getPasswordHash())) {
			throw new IllegalArgumentException("Tên đăng nhập hoặc mật khẩu không chính xác");
		}

		Long walletId = loadUserPort.findWalletIdByUserId(user.getId().getValue());
		String token = tokenGeneratorPort.generateToken(user.getUsername(), user.getId().getValue(), walletId);

		return new LoginResult(user.getId().getValue(), user.getUsername(), walletId, token);
	}
}
