package io.reflectoring.digitalwallet.application.port.in;

import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;
import io.reflectoring.digitalwallet.application.domain.model.Money;

public interface GetAccountBalanceUseCase {

	Money getAccountBalance(GetAccountBalanceQuery query);

	record GetAccountBalanceQuery(AccountId accountId) {
	}
}
