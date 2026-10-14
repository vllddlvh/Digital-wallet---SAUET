package io.reflectoring.digitalwallet.adapter.in.web.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAdapterTest {

	private PasswordEncoder passwordEncoder;
	private JwtTokenProvider jwtTokenProvider;
	private SecurityAdapter securityAdapter;

	@BeforeEach
	void setUp() {
		passwordEncoder = new BCryptPasswordEncoder();
		jwtTokenProvider = new JwtTokenProvider();
		securityAdapter = new SecurityAdapter(passwordEncoder, jwtTokenProvider);
	}

	@Test
	void encodeAndMatches() {
		String raw = "P@ssword123!";
		String encoded = securityAdapter.encode(raw);

		assertThat(encoded).isNotEqualTo(raw);
		assertThat(securityAdapter.matches(raw, encoded)).isTrue();
		assertThat(securityAdapter.matches("wrongPassword", encoded)).isFalse();
	}

	@Test
	void generateToken() {
		String token = securityAdapter.generateToken("user_alice", 10L, 42L);

		assertThat(token).isNotBlank();
		assertThat(jwtTokenProvider.validateToken(token)).isTrue();
		assertThat(jwtTokenProvider.getUsername(token)).isEqualTo("user_alice");
	}
}
