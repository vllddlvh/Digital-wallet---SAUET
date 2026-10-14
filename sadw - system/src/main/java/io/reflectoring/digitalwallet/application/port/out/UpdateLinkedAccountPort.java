package io.reflectoring.digitalwallet.application.port.out;

import io.reflectoring.digitalwallet.application.domain.model.LinkedBankAccount;

public interface UpdateLinkedAccountPort {

	LinkedBankAccount saveLinkedAccount(LinkedBankAccount linkedBankAccount);

	boolean unlinkAccount(Long linkedAccountId);
}
