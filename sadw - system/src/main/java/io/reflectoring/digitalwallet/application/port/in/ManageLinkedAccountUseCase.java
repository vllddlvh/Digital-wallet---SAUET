package io.reflectoring.digitalwallet.application.port.in;

import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;
import io.reflectoring.digitalwallet.application.domain.model.LinkedBankAccount;

import java.util.List;

public interface ManageLinkedAccountUseCase {

	List<LinkedBankAccount> getLinkedAccounts(AccountId walletId);

	LinkedBankAccount linkBankAccount(LinkBankAccountCommand command);

	record LinkBankAccountCommand(
			AccountId walletId,
			String bankCode,
			String bankName,
			String accountNumber,
			String accountHolderName
	) {
		public LinkBankAccountCommand {
			if (walletId == null) {
				throw new IllegalArgumentException("Wallet ID không được để trống");
			}
			if (bankCode == null || bankCode.isBlank()) {
				throw new IllegalArgumentException("Bank code không được để trống");
			}
			if (accountNumber == null || accountNumber.isBlank()) {
				throw new IllegalArgumentException("Số tài khoản ngân hàng không được để trống");
			}
			if (accountHolderName == null || accountHolderName.isBlank()) {
				throw new IllegalArgumentException("Tên chủ tài khoản không được để trống");
			}
		}
	}
}
