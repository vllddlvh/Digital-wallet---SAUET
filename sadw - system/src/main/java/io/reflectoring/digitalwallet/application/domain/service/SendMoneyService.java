package io.reflectoring.digitalwallet.application.domain.service;

import io.reflectoring.digitalwallet.application.port.in.SendMoneyCommand;
import io.reflectoring.digitalwallet.application.port.in.SendMoneyUseCase;
import io.reflectoring.digitalwallet.application.port.out.AccountLock;
import io.reflectoring.digitalwallet.application.port.out.LoadAccountPort;
import io.reflectoring.digitalwallet.application.port.out.UpdateAccountStatePort;
import io.reflectoring.digitalwallet.common.UseCase;
import io.reflectoring.digitalwallet.application.domain.model.Account;
import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;
import lombok.RequiredArgsConstructor;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@UseCase
@Transactional
public class SendMoneyService implements SendMoneyUseCase {

	private final LoadAccountPort loadAccountPort;
	private final AccountLock accountLock;
	private final UpdateAccountStatePort updateAccountStatePort;
	private final MoneyTransferProperties moneyTransferProperties;

	@Override
	public boolean sendMoney(SendMoneyCommand command) {

		checkThreshold(command);

		LocalDateTime baselineDate = LocalDateTime.now().minusDays(10);

		Account sourceAccount = loadAccountPort.loadAccount(
				command.sourceAccountId(),
				baselineDate);

		Account targetAccount = loadAccountPort.loadAccount(
				command.targetAccountId(),
				baselineDate);

		AccountId sourceAccountId = sourceAccount.getId()
				.orElseThrow(() -> new IllegalStateException("expected source account ID not to be empty"));
		AccountId targetAccountId = targetAccount.getId()
				.orElseThrow(() -> new IllegalStateException("expected target account ID not to be empty"));

		accountLock.lockAccount(sourceAccountId);
		if (!sourceAccount.withdraw(command.money(), targetAccountId)) {
			accountLock.releaseAccount(sourceAccountId);
			return false;
		}

		accountLock.lockAccount(targetAccountId);
		if (!targetAccount.deposit(command.money(), sourceAccountId)) {
			accountLock.releaseAccount(sourceAccountId);
			accountLock.releaseAccount(targetAccountId);
			return false;
		}

		updateAccountStatePort.updateActivities(sourceAccount);
		updateAccountStatePort.updateActivities(targetAccount);

		accountLock.releaseAccount(sourceAccountId);
		accountLock.releaseAccount(targetAccountId);
		return true;
	}

	private void checkThreshold(SendMoneyCommand command) {
		if(command.money().isGreaterThan(moneyTransferProperties.getMaximumTransferThreshold())){
			throw new ThresholdExceededException(moneyTransferProperties.getMaximumTransferThreshold(), command.money());
		}
	}

}




