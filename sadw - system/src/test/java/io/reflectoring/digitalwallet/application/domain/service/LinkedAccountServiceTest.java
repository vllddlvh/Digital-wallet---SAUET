package io.reflectoring.digitalwallet.application.domain.service;

import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;
import io.reflectoring.digitalwallet.application.domain.model.LinkedBankAccount;
import io.reflectoring.digitalwallet.application.domain.model.LinkedBankAccount.LinkedAccountId;
import io.reflectoring.digitalwallet.application.port.in.ManageLinkedAccountUseCase.LinkBankAccountCommand;
import io.reflectoring.digitalwallet.application.port.in.UnlinkBankAccountUseCase.UnlinkBankAccountCommand;
import io.reflectoring.digitalwallet.application.port.out.LoadLinkedAccountPort;
import io.reflectoring.digitalwallet.application.port.out.UpdateLinkedAccountPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

class LinkedAccountServiceTest {

	private LoadLinkedAccountPort loadLinkedAccountPort;
	private UpdateLinkedAccountPort updateLinkedAccountPort;
	private LinkedAccountService service;

	@BeforeEach
	void setUp() {
		loadLinkedAccountPort = Mockito.mock(LoadLinkedAccountPort.class);
		updateLinkedAccountPort = Mockito.mock(UpdateLinkedAccountPort.class);
		service = new LinkedAccountService(loadLinkedAccountPort, updateLinkedAccountPort);
	}

	@Test
	void unlinkBankAccount_successful() {
		UnlinkBankAccountCommand command = new UnlinkBankAccountCommand(new AccountId(1L), 10L);
		LinkedBankAccount account = new LinkedBankAccount(
				new LinkedAccountId(10L),
				new AccountId(1L),
				"VCB",
				"Vietcombank",
				"0123456789",
				"NGUYEN VAN A",
				"ACTIVE"
		);

		given(loadLinkedAccountPort.loadLinkedAccount(10L)).willReturn(Optional.of(account));
		given(updateLinkedAccountPort.unlinkAccount(10L)).willReturn(true);

		boolean result = service.unlinkBankAccount(command);

		assertThat(result).isTrue();
	}

	@Test
	void unlinkBankAccount_wrongWallet_throwsException() {
		UnlinkBankAccountCommand command = new UnlinkBankAccountCommand(new AccountId(2L), 10L);
		LinkedBankAccount account = new LinkedBankAccount(
				new LinkedAccountId(10L),
				new AccountId(1L),
				"VCB",
				"Vietcombank",
				"0123456789",
				"NGUYEN VAN A",
				"ACTIVE"
		);

		given(loadLinkedAccountPort.loadLinkedAccount(10L)).willReturn(Optional.of(account));

		assertThatThrownBy(() -> service.unlinkBankAccount(command))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("không thuộc sở hữu");
	}

	@Test
	void linkBankAccount_successful() {
		LinkBankAccountCommand command = new LinkBankAccountCommand(
				new AccountId(1L),
				"VCB",
				"Vietcombank",
				"0123456789",
				"NGUYEN VAN A"
		);

		LinkedBankAccount saved = new LinkedBankAccount(
				new LinkedAccountId(10L),
				new AccountId(1L),
				"VCB",
				"Vietcombank",
				"0123456789",
				"NGUYEN VAN A",
				"ACTIVE"
		);

		given(updateLinkedAccountPort.saveLinkedAccount(any(LinkedBankAccount.class))).willReturn(saved);

		LinkedBankAccount result = service.linkBankAccount(command);

		assertThat(result.getId().getValue()).isEqualTo(10L);
		assertThat(result.getStatus()).isEqualTo("ACTIVE");
	}

	@Test
	void getLinkedAccounts_successful() {
		LinkedBankAccount account = new LinkedBankAccount(
				new LinkedAccountId(10L),
				new AccountId(1L),
				"VCB",
				"Vietcombank",
				"0123456789",
				"NGUYEN VAN A",
				"ACTIVE"
		);

		given(loadLinkedAccountPort.loadLinkedAccounts(new AccountId(1L))).willReturn(List.of(account));

		List<LinkedBankAccount> list = service.getLinkedAccounts(new AccountId(1L));

		assertThat(list).hasSize(1);
		assertThat(list.get(0).getBankCode()).isEqualTo("VCB");
	}
}
