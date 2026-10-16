package sa.uet.digitalwallet_system.application.port.in;

import sa.uet.digitalwallet_system.domain.model.Transaction;

public interface WithdrawUseCase {
    Transaction withdraw(WithdrawCommand command);
}
