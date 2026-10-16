package sa.uet.digitalwallet_system.application.port.out;

import sa.uet.digitalwallet_system.domain.model.LinkedBankAccount;

import java.util.List;
import java.util.Optional;

public interface LoadLinkedBankPort {
    List<LinkedBankAccount> loadActiveByWalletId(Long walletId);
    Optional<LinkedBankAccount> loadByIdAndWalletId(Long id, Long walletId);
    Optional<LinkedBankAccount> loadById(Long id);
}
