package io.reflectoring.digitalwallet.application.domain.service;

import io.reflectoring.digitalwallet.application.domain.model.Money;
import io.reflectoring.digitalwallet.application.port.in.GetAccountBalanceUseCase;
import io.reflectoring.digitalwallet.application.port.out.LoadAccountPort;
import io.reflectoring.digitalwallet.common.UseCase;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@UseCase
public class GetAccountBalanceService implements GetAccountBalanceUseCase {

	private final LoadAccountPort loadAccountPort;

	@Override
	public Money getAccountBalance(GetAccountBalanceQuery query) {
		return loadAccountPort.loadAccount(query.accountId(), LocalDateTime.now())
				.calculateBalance();
	}
}
