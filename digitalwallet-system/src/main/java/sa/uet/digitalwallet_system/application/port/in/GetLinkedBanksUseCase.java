package sa.uet.digitalwallet_system.application.port.in;

import sa.uet.digitalwallet_system.domain.model.LinkedBankAccount;

import java.util.List;

public interface GetLinkedBanksUseCase {
    List<LinkedBankAccount> getLinkedBanks(Long walletId);
}
