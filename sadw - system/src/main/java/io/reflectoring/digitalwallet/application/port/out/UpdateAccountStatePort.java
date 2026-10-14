package io.reflectoring.digitalwallet.application.port.out;

import io.reflectoring.digitalwallet.application.domain.model.Account;

public interface UpdateAccountStatePort {

	void updateActivities(Account account);

}
