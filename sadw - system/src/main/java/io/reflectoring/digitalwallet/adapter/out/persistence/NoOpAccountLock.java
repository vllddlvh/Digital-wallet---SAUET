package io.reflectoring.digitalwallet.adapter.out.persistence;

import io.reflectoring.digitalwallet.application.port.out.AccountLock;
import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;
import org.springframework.stereotype.Component;

@Component
class NoOpAccountLock implements AccountLock {

	@Override
	public void lockAccount(AccountId accountId) {
		// do nothing
	}

	@Override
	public void releaseAccount(AccountId accountId) {
		// do nothing
	}

}
