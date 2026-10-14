package io.reflectoring.digitalwallet.application.domain.service;

import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;
import io.reflectoring.digitalwallet.application.domain.model.LinkedBankAccount;
import io.reflectoring.digitalwallet.application.domain.model.LinkedBankAccount.LinkedAccountId;
import io.reflectoring.digitalwallet.application.port.in.ManageLinkedAccountUseCase;
import io.reflectoring.digitalwallet.application.port.in.UnlinkBankAccountUseCase;
import io.reflectoring.digitalwallet.application.port.out.LoadLinkedAccountPort;
import io.reflectoring.digitalwallet.application.port.out.UpdateLinkedAccountPort;
import io.reflectoring.digitalwallet.common.UseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@UseCase
@Transactional
public class LinkedAccountService implements UnlinkBankAccountUseCase, ManageLinkedAccountUseCase {

	private final LoadLinkedAccountPort loadLinkedAccountPort;
	private final UpdateLinkedAccountPort updateLinkedAccountPort;

	@Override
	public List<LinkedBankAccount> getLinkedAccounts(AccountId walletId) {
		return loadLinkedAccountPort.loadLinkedAccounts(walletId);
	}

	@Override
	public LinkedBankAccount linkBankAccount(LinkBankAccountCommand command) {
		LinkedBankAccount newAccount = new LinkedBankAccount(
				null,
				command.walletId(),
				command.bankCode(),
				command.bankName(),
				command.accountNumber(),
				command.accountHolderName(),
				"ACTIVE"
		);
		return updateLinkedAccountPort.saveLinkedAccount(newAccount);
	}

	@Override
	public boolean unlinkBankAccount(UnlinkBankAccountCommand command) {
		LinkedBankAccount account = loadLinkedAccountPort.loadLinkedAccount(command.linkedAccountId())
				.orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản ngân hàng liên kết với ID: " + command.linkedAccountId()));

		if (!account.getWalletId().equals(command.walletId())) {
			throw new IllegalStateException("Tài khoản ngân hàng liên kết này không thuộc sở hữu của ví ID: " + command.walletId().getValue());
		}

		if ("UNLINKED".equalsIgnoreCase(account.getStatus())) {
			throw new IllegalStateException("Tài khoản ngân hàng này đã bị hủy liên kết từ trước");
		}

		return updateLinkedAccountPort.unlinkAccount(command.linkedAccountId());
	}
}
