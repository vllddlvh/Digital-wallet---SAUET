package sa.uet.digitalwallet_system.application.port.out;

import sa.uet.digitalwallet_system.domain.model.User;

public interface TokenGeneratorPort {
    String generateToken(User user);
}
