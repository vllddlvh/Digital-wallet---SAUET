package io.reflectoring.digitalwallet.application.port.out;

import io.reflectoring.digitalwallet.application.domain.model.Account;

public interface AccountLock {

	void lockAccount(Account.AccountId accountId);

	void releaseAccount(Account.AccountId accountId);

}
