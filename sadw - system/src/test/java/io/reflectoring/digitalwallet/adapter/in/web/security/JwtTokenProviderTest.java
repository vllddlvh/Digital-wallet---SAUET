package io.reflectoring.digitalwallet.adapter.in.web.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

	private JwtTokenProvider tokenProvider;

	@BeforeEach
	void setUp() {
		tokenProvider = new JwtTokenProvider();
	}

	@Test
	void generateToken_andValidate_success() {
		String token = tokenProvider.generateToken("john_doe", 10L, 42L);

		assertThat(token).isNotBlank();
		assertThat(tokenProvider.validateToken(token)).isTrue();
		assertThat(tokenProvider.getUsername(token)).isEqualTo("john_doe");
		assertThat(tokenProvider.getUserId(token)).isEqualTo(10L);
		assertThat(tokenProvider.getWalletId(token)).isEqualTo(42L);
	}

	@Test
	void validateToken_invalidToken_returnsFalse() {
		assertThat(tokenProvider.validateToken("invalid.token.structure")).isFalse();
		assertThat(tokenProvider.validateToken("")).isFalse();
		assertThat(tokenProvider.validateToken(null)).isFalse();
	}

	@Test
	void validateToken_tamperedToken_returnsFalse() {
		String token = tokenProvider.generateToken("alice", 1L, 100L);
		String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

		assertThat(tokenProvider.validateToken(tamperedToken)).isFalse();
	}
}
