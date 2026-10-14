package io.reflectoring.digitalwallet.adapter.out.persistence;

import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;
import io.reflectoring.digitalwallet.application.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(UserPersistenceAdapter.class)
class UserPersistenceAdapterTest {

	@Autowired
	private UserPersistenceAdapter adapterUnderTest;

	@Test
	void createUser_andLoadUserByUsername() {
		User user = new User(
				null,
				"johndoe",
				"hashed_pass_123",
				"John Doe",
				"john@digitalwallet.io",
				"ROLE_USER"
		);

		User saved = adapterUnderTest.createUser(user);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getUsername()).isEqualTo("johndoe");

		Optional<User> loaded = adapterUnderTest.loadUserByUsername("johndoe");
		assertThat(loaded).isPresent();
		assertThat(loaded.get().getFullName()).isEqualTo("John Doe");
		assertThat(loaded.get().getEmail()).isEqualTo("john@digitalwallet.io");
	}

	@Test
	void existsByUsername() {
		User user = new User(
				null,
				"existinguser",
				"hashed_pass",
				"Existing User",
				"exist@digitalwallet.io",
				"ROLE_USER"
		);
		adapterUnderTest.createUser(user);

		assertThat(adapterUnderTest.existsByUsername("existinguser")).isTrue();
		assertThat(adapterUnderTest.existsByUsername("nonexistent")).isFalse();
	}

	@Test
	void createAccount_andFindWalletIdByUserId() {
		User user = new User(
				null,
				"walletuser",
				"hashed_pass",
				"Wallet User",
				"wallet@digitalwallet.io",
				"ROLE_USER"
		);
		User saved = adapterUnderTest.createUser(user);
		Long userId = saved.getId().getValue();

		AccountId walletId = adapterUnderTest.createAccount(userId);
		assertThat(walletId).isNotNull();

		Long foundWalletId = adapterUnderTest.findWalletIdByUserId(userId);
		assertThat(foundWalletId).isEqualTo(walletId.getValue());

		assertThat(adapterUnderTest.findWalletIdByUserId(99999L)).isNull();
	}
}
