package io.reflectoring.digitalwallet.application.port.out;

import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;
import io.reflectoring.digitalwallet.application.domain.model.LinkedBankAccount;

import java.util.List;
import java.util.Optional;

public interface LoadLinkedAccountPort {

	List<LinkedBankAccount> loadLinkedAccounts(AccountId walletId);

	Optional<LinkedBankAccount> loadLinkedAccount(Long linkedAccountId);
}
