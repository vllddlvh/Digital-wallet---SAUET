package io.reflectoring.digitalwallet.application.port.out;

import java.time.LocalDateTime;

import io.reflectoring.digitalwallet.application.domain.model.Account;
import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;

public interface LoadAccountPort {

	Account loadAccount(AccountId accountId, LocalDateTime baselineDate);
}
