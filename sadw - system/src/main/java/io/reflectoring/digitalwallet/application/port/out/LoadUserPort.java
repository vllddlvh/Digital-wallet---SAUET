package io.reflectoring.digitalwallet.application.port.out;

import io.reflectoring.digitalwallet.application.domain.model.User;
import java.util.Optional;

public interface LoadUserPort {
	Optional<User> loadUserByUsername(String username);
	boolean existsByUsername(String username);
	Long findWalletIdByUserId(Long userId);
}
