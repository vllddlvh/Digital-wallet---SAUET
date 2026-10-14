package io.reflectoring.digitalwallet.application.domain.model;

import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;
import lombok.Value;

@Value
public class LinkedBankAccount {

	LinkedAccountId id;
	AccountId walletId;
	String bankCode;
	String bankName;
	String accountNumber;
	String accountHolderName;
	String status;

	@Value
	public static class LinkedAccountId {
		Long value;
	}
}
