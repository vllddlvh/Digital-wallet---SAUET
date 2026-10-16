package sa.uet.digitalwallet_system.application.port.out;

import sa.uet.digitalwallet_system.domain.model.User;

public interface SaveUserPort {
    User save(User user);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
