package sa.uet.digitalwallet_system.application.port.in;

import sa.uet.digitalwallet_system.domain.model.Transaction;

public interface TransferUseCase {
    Transaction transfer(TransferCommand command);
}
