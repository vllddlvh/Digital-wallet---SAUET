package io.reflectoring.digitalwallet.application.port.out;

import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;

public interface CreateAccountPort {
	AccountId createAccount(Long userId);
}
