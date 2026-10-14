package io.reflectoring.digitalwallet.application.domain.service;

import io.reflectoring.digitalwallet.application.domain.model.Account;
import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;
import io.reflectoring.digitalwallet.application.domain.model.ActivityWindow;
import io.reflectoring.digitalwallet.application.domain.model.Money;
import io.reflectoring.digitalwallet.application.port.in.GetAccountBalanceUseCase.GetAccountBalanceQuery;
import io.reflectoring.digitalwallet.application.port.out.LoadAccountPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static io.reflectoring.digitalwallet.common.AccountTestData.defaultAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

class GetAccountBalanceServiceTest {

	private LoadAccountPort loadAccountPort;
	private GetAccountBalanceService service;

	@BeforeEach
	void setUp() {
		loadAccountPort = Mockito.mock(LoadAccountPort.class);
		service = new GetAccountBalanceService(loadAccountPort);
	}

	@Test
	void getAccountBalance_successful() {
		AccountId accountId = new AccountId(42L);
		Account account = defaultAccount()
				.withAccountId(accountId)
				.withBaselineBalance(Money.of(1500000L))
				.withActivityWindow(new ActivityWindow())
				.build();

		given(loadAccountPort.loadAccount(eq(accountId), any(LocalDateTime.class)))
				.willReturn(account);

		GetAccountBalanceQuery query = new GetAccountBalanceQuery(accountId);
		Money balance = service.getAccountBalance(query);

		assertThat(balance).isEqualTo(Money.of(1500000L));
	}
}
