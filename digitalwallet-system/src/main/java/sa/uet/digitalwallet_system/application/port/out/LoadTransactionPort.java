package sa.uet.digitalwallet_system.application.port.out;

import sa.uet.digitalwallet_system.domain.model.Transaction;

import java.util.List;

public interface LoadTransactionPort {
    List<Transaction> loadByWalletId(Long walletId);
}
