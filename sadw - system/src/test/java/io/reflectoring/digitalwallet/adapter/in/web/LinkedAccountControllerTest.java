package io.reflectoring.digitalwallet.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;
import io.reflectoring.digitalwallet.application.domain.model.LinkedBankAccount;
import io.reflectoring.digitalwallet.application.domain.model.LinkedBankAccount.LinkedAccountId;
import io.reflectoring.digitalwallet.application.port.in.ManageLinkedAccountUseCase;
import io.reflectoring.digitalwallet.application.port.in.ManageLinkedAccountUseCase.LinkBankAccountCommand;
import io.reflectoring.digitalwallet.application.port.in.UnlinkBankAccountUseCase;
import io.reflectoring.digitalwallet.application.port.in.UnlinkBankAccountUseCase.UnlinkBankAccountCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LinkedAccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class LinkedAccountControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private ManageLinkedAccountUseCase manageLinkedAccountUseCase;

	@MockBean
	private UnlinkBankAccountUseCase unlinkBankAccountUseCase;

	@Test
	void testGetLinkedAccounts() throws Exception {
		LinkedBankAccount account = new LinkedBankAccount(
				new LinkedAccountId(1L),
				new AccountId(42L),
				"VCB",
				"Vietcombank",
				"0123456789",
				"NGUYEN VAN A",
				"ACTIVE"
		);

		given(manageLinkedAccountUseCase.getLinkedAccounts(any(AccountId.class)))
				.willReturn(List.of(account));

		mockMvc.perform(get("/api/v1/wallets/{walletId}/linked-accounts", 42L)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data[0].id").value(1))
				.andExpect(jsonPath("$.data[0].bankCode").value("VCB"))
				.andExpect(jsonPath("$.data[0].accountNumber").value("******6789"));
	}

	@Test
	void testLinkAccount() throws Exception {
		LinkBankAccountRequest request = new LinkBankAccountRequest(
				"VCB",
				"Vietcombank",
				"0123456789",
				"NGUYEN VAN A"
		);

		LinkedBankAccount saved = new LinkedBankAccount(
				new LinkedAccountId(5L),
				new AccountId(42L),
				"VCB",
				"Vietcombank",
				"0123456789",
				"NGUYEN VAN A",
				"ACTIVE"
		);

		given(manageLinkedAccountUseCase.linkBankAccount(any(LinkBankAccountCommand.class)))
				.willReturn(saved);

		mockMvc.perform(post("/api/v1/wallets/{walletId}/linked-accounts", 42L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(5))
				.andExpect(jsonPath("$.data.bankCode").value("VCB"));
	}

	@Test
	void testUnlinkAccount_successful() throws Exception {
		given(unlinkBankAccountUseCase.unlinkBankAccount(any(UnlinkBankAccountCommand.class)))
				.willReturn(true);

		mockMvc.perform(delete("/api/v1/wallets/{walletId}/linked-accounts/{accountId}", 42L, 5L)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.unlinkedAccountId").value(5))
				.andExpect(jsonPath("$.data.walletId").value(42))
				.andExpect(jsonPath("$.data.status").value("UNLINKED"));
	}

	@Test
	void testLinkAccount_validationFailure() throws Exception {
		LinkBankAccountRequest request = new LinkBankAccountRequest("", "", "", "");

		mockMvc.perform(post("/api/v1/wallets/{walletId}/linked-accounts", 42L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
	}

	@Test
	void testUnlinkAccount_notFound() throws Exception {
		given(unlinkBankAccountUseCase.unlinkBankAccount(any(UnlinkBankAccountCommand.class)))
				.willReturn(false);

		mockMvc.perform(delete("/api/v1/wallets/{walletId}/linked-accounts/{accountId}", 42L, 999L)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
	}
}
