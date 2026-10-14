package io.reflectoring.digitalwallet.adapter.out.persistence;

import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;
import io.reflectoring.digitalwallet.application.domain.model.LinkedBankAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(LinkedAccountPersistenceAdapter.class)
class LinkedAccountPersistenceAdapterTest {

	@Autowired
	private LinkedAccountPersistenceAdapter adapterUnderTest;

	@Test
	void saveLinkedAccount_andLoadByWalletId() {
		AccountId walletId = new AccountId(10L);
		LinkedBankAccount account = new LinkedBankAccount(
				null,
				walletId,
				"VCB",
				"Vietcombank",
				"0123456789",
				"NGUYEN VAN A",
				"ACTIVE"
		);

		LinkedBankAccount saved = adapterUnderTest.saveLinkedAccount(account);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getBankCode()).isEqualTo("VCB");
		assertThat(saved.getStatus()).isEqualTo("ACTIVE");

		List<LinkedBankAccount> accounts = adapterUnderTest.loadLinkedAccounts(walletId);
		assertThat(accounts).hasSize(1);
		assertThat(accounts.get(0).getAccountNumber()).isEqualTo("0123456789");
	}

	@Test
	void loadLinkedAccount_byId() {
		AccountId walletId = new AccountId(20L);
		LinkedBankAccount account = new LinkedBankAccount(
				null,
				walletId,
				"TCB",
				"Techcombank",
				"9876543210",
				"TRAN VAN B",
				"ACTIVE"
		);

		LinkedBankAccount saved = adapterUnderTest.saveLinkedAccount(account);
		Long savedId = saved.getId().getValue();

		Optional<LinkedBankAccount> loaded = adapterUnderTest.loadLinkedAccount(savedId);
		assertThat(loaded).isPresent();
		assertThat(loaded.get().getBankCode()).isEqualTo("TCB");
		assertThat(loaded.get().getAccountHolderName()).isEqualTo("TRAN VAN B");
	}

	@Test
	void unlinkAccount_successAndNotFound() {
		AccountId walletId = new AccountId(30L);
		LinkedBankAccount account = new LinkedBankAccount(
				null,
				walletId,
				"BIDV",
				"BIDV Bank",
				"1122334455",
				"LE VAN C",
				"ACTIVE"
		);

		LinkedBankAccount saved = adapterUnderTest.saveLinkedAccount(account);
		Long savedId = saved.getId().getValue();

		boolean unlinked = adapterUnderTest.unlinkAccount(savedId);
		assertThat(unlinked).isTrue();

		Optional<LinkedBankAccount> updated = adapterUnderTest.loadLinkedAccount(savedId);
		assertThat(updated).isPresent();
		assertThat(updated.get().getStatus()).isEqualTo("UNLINKED");

		boolean nonExistentUnlinked = adapterUnderTest.unlinkAccount(99999L);
		assertThat(nonExistentUnlinked).isFalse();
	}
}
