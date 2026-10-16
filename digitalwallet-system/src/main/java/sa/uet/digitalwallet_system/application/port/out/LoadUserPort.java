package sa.uet.digitalwallet_system.application.port.out;

import sa.uet.digitalwallet_system.domain.model.User;

import java.util.Optional;

public interface LoadUserPort {
    Optional<User> loadByUsername(String username);
}
