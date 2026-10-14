package io.reflectoring.digitalwallet.adapter.in.web.security;

import io.reflectoring.digitalwallet.application.port.out.PasswordEncoderPort;
import io.reflectoring.digitalwallet.application.port.out.TokenGeneratorPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityAdapter implements PasswordEncoderPort, TokenGeneratorPort {

	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;

	@Override
	public String encode(String rawPassword) {
		return passwordEncoder.encode(rawPassword);
	}

	@Override
	public boolean matches(String rawPassword, String encodedPassword) {
		return passwordEncoder.matches(rawPassword, encodedPassword);
	}

	@Override
	public String generateToken(String username, Long userId, Long walletId) {
		return jwtTokenProvider.generateToken(username, userId, walletId);
	}
}
