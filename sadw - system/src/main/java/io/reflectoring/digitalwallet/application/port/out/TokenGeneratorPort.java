package io.reflectoring.digitalwallet.application.port.out;

public interface TokenGeneratorPort {
	String generateToken(String username, Long userId, Long walletId);
}
