package io.reflectoring.digitalwallet.application.domain;

import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;
import io.reflectoring.digitalwallet.application.domain.model.LinkedBankAccount;
import io.reflectoring.digitalwallet.application.domain.model.LinkedBankAccount.LinkedAccountId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LinkedBankAccountTest {

	@Test
	void linkedBankAccountCreationAndGetters() {
		LinkedAccountId id = new LinkedAccountId(100L);
		AccountId walletId = new AccountId(42L);

		LinkedBankAccount account = new LinkedBankAccount(
				id,
				walletId,
				"VCB",
				"Vietcombank",
				"0123456789",
				"NGUYEN VAN A",
				"ACTIVE"
		);

		assertThat(account.getId()).isEqualTo(id);
		assertThat(account.getId().getValue()).isEqualTo(100L);
		assertThat(account.getWalletId()).isEqualTo(walletId);
		assertThat(account.getWalletId().getValue()).isEqualTo(42L);
		assertThat(account.getBankCode()).isEqualTo("VCB");
		assertThat(account.getBankName()).isEqualTo("Vietcombank");
		assertThat(account.getAccountNumber()).isEqualTo("0123456789");
		assertThat(account.getAccountHolderName()).isEqualTo("NGUYEN VAN A");
		assertThat(account.getStatus()).isEqualTo("ACTIVE");
	}

	@Test
	void linkedAccountIdEquality() {
		LinkedAccountId id1 = new LinkedAccountId(5L);
		LinkedAccountId id2 = new LinkedAccountId(5L);
		LinkedAccountId id3 = new LinkedAccountId(10L);

		assertThat(id1).isEqualTo(id2);
		assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
		assertThat(id1).isNotEqualTo(id3);
	}

	@Test
	void valueObjectEquality() {
		LinkedBankAccount a1 = new LinkedBankAccount(
				new LinkedAccountId(1L), new AccountId(2L), "VCB", "Vietcombank", "123", "NAME", "ACTIVE");
		LinkedBankAccount a2 = new LinkedBankAccount(
				new LinkedAccountId(1L), new AccountId(2L), "VCB", "Vietcombank", "123", "NAME", "ACTIVE");
		LinkedBankAccount a3 = new LinkedBankAccount(
				new LinkedAccountId(2L), new AccountId(2L), "VCB", "Vietcombank", "123", "NAME", "ACTIVE");

		assertThat(a1).isEqualTo(a2);
		assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
		assertThat(a1).isNotEqualTo(a3);
	}
}
