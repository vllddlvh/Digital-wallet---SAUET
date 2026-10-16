package sa.uet.digitalwallet_system.application.port.in;

import sa.uet.digitalwallet_system.domain.model.LinkedBankAccount;

public interface LinkBankUseCase {
    LinkedBankAccount link(LinkBankCommand command);
}
