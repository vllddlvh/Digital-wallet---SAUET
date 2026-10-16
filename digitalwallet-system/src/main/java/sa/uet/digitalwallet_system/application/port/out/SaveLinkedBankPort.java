package sa.uet.digitalwallet_system.application.port.out;

import sa.uet.digitalwallet_system.domain.model.LinkedBankAccount;

public interface SaveLinkedBankPort {
    LinkedBankAccount save(LinkedBankAccount linkedBankAccount);
}
