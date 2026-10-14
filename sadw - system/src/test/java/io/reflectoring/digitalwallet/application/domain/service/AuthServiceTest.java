package io.reflectoring.digitalwallet.application.domain.service;

import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;
import io.reflectoring.digitalwallet.application.domain.model.User;
import io.reflectoring.digitalwallet.application.domain.model.User.UserId;
import io.reflectoring.digitalwallet.application.port.in.LoginUseCase.LoginCommand;
import io.reflectoring.digitalwallet.application.port.in.LoginUseCase.LoginResult;
import io.reflectoring.digitalwallet.application.port.in.RegisterUserUseCase.RegisterUserCommand;
import io.reflectoring.digitalwallet.application.port.out.CreateAccountPort;
import io.reflectoring.digitalwallet.application.port.out.CreateUserPort;
import io.reflectoring.digitalwallet.application.port.out.LoadUserPort;
import io.reflectoring.digitalwallet.application.port.out.PasswordEncoderPort;
import io.reflectoring.digitalwallet.application.port.out.TokenGeneratorPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

class AuthServiceTest {

	private LoadUserPort loadUserPort;
	private CreateUserPort createUserPort;
	private CreateAccountPort createAccountPort;
	private PasswordEncoderPort passwordEncoderPort;
	private TokenGeneratorPort tokenGeneratorPort;
	private AuthService authService;

	@BeforeEach
	void setUp() {
		loadUserPort = Mockito.mock(LoadUserPort.class);
		createUserPort = Mockito.mock(CreateUserPort.class);
		createAccountPort = Mockito.mock(CreateAccountPort.class);
		passwordEncoderPort = Mockito.mock(PasswordEncoderPort.class);
		tokenGeneratorPort = Mockito.mock(TokenGeneratorPort.class);

		authService = new AuthService(
				loadUserPort,
				createUserPort,
				createAccountPort,
				passwordEncoderPort,
				tokenGeneratorPort
		);
	}

	@Test
	void registerUser_successful() {
		RegisterUserCommand command = new RegisterUserCommand(
				"testuser",
				"password123",
				"Test User",
				"test@digitalwallet.io"
		);

		given(loadUserPort.existsByUsername("testuser")).willReturn(false);
		given(passwordEncoderPort.encode("password123")).willReturn("hashedPassword");
		given(createUserPort.createUser(any(User.class)))
				.willReturn(new User(new UserId(10L), "testuser", "hashedPassword", "Test User", "test@digitalwallet.io", "ROLE_USER"));
		given(createAccountPort.createAccount(10L)).willReturn(new AccountId(42L));

		Long userId = authService.registerUser(command);

		assertThat(userId).isEqualTo(10L);
	}

	@Test
	void registerUser_duplicateUsername_throwsException() {
		RegisterUserCommand command = new RegisterUserCommand(
				"existinguser",
				"password123",
				"Test User",
				"test@digitalwallet.io"
		);

		given(loadUserPort.existsByUsername("existinguser")).willReturn(true);

		assertThatThrownBy(() -> authService.registerUser(command))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("đã tồn tại");
	}

	@Test
	void login_successful() {
		LoginCommand command = new LoginCommand("testuser", "password123");
		User user = new User(new UserId(1L), "testuser", "hashedPassword", "Test User", "test@digitalwallet.io", "ROLE_USER");

		given(loadUserPort.loadUserByUsername("testuser")).willReturn(Optional.of(user));
		given(passwordEncoderPort.matches("password123", "hashedPassword")).willReturn(true);
		given(loadUserPort.findWalletIdByUserId(1L)).willReturn(42L);
		given(tokenGeneratorPort.generateToken(eq("testuser"), eq(1L), eq(42L))).willReturn("mock.jwt.token");

		LoginResult result = authService.login(command);

		assertThat(result.userId()).isEqualTo(1L);
		assertThat(result.walletId()).isEqualTo(42L);
		assertThat(result.token()).isEqualTo("mock.jwt.token");
	}

	@Test
	void login_wrongPassword_throwsException() {
		LoginCommand command = new LoginCommand("testuser", "wrongpassword");
		User user = new User(new UserId(1L), "testuser", "hashedPassword", "Test User", "test@digitalwallet.io", "ROLE_USER");

		given(loadUserPort.loadUserByUsername("testuser")).willReturn(Optional.of(user));
		given(passwordEncoderPort.matches("wrongpassword", "hashedPassword")).willReturn(false);

		assertThatThrownBy(() -> authService.login(command))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("không chính xác");
	}
}
