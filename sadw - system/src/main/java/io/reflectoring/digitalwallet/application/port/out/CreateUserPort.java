package io.reflectoring.digitalwallet.application.port.out;

import io.reflectoring.digitalwallet.application.domain.model.User;

public interface CreateUserPort {
	User createUser(User user);
}
