package sa.uet.digitalwallet_system.application.port.in;

import sa.uet.digitalwallet_system.domain.model.Transaction;

import java.util.List;

public interface GetTransactionsUseCase {
    List<Transaction> getTransactions(Long walletId);
}
