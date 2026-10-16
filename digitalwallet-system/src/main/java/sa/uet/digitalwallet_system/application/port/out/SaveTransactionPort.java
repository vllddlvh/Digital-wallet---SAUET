package sa.uet.digitalwallet_system.application.port.out;

import sa.uet.digitalwallet_system.domain.model.Transaction;

public interface SaveTransactionPort {
    Transaction save(Transaction transaction);
}
