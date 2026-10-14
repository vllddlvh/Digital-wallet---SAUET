package io.reflectoring.digitalwallet.application.port.in;

import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;

public interface UnlinkBankAccountUseCase {

	boolean unlinkBankAccount(UnlinkBankAccountCommand command);

	record UnlinkBankAccountCommand(
			AccountId walletId,
			Long linkedAccountId
	) {
		public UnlinkBankAccountCommand {
			if (walletId == null) {
				throw new IllegalArgumentException("Wallet ID không được để trống");
			}
			if (linkedAccountId == null) {
				throw new IllegalArgumentException("Linked Account ID không được để trống");
			}
		}
	}
}
