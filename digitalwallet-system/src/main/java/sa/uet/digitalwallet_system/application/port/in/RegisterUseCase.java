package sa.uet.digitalwallet_system.application.port.in;

import sa.uet.digitalwallet_system.domain.model.User;

public interface RegisterUseCase {
    User register(RegisterCommand command);
}
