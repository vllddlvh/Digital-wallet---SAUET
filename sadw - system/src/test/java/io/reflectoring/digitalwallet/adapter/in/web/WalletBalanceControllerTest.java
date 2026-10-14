package io.reflectoring.digitalwallet.adapter.in.web;

import io.reflectoring.digitalwallet.application.domain.model.Money;
import io.reflectoring.digitalwallet.application.port.in.GetAccountBalanceUseCase;
import io.reflectoring.digitalwallet.application.port.in.GetAccountBalanceUseCase.GetAccountBalanceQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WalletBalanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class WalletBalanceControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private GetAccountBalanceUseCase getAccountBalanceUseCase;

	@Test
	void testGetBalance() throws Exception {
		given(getAccountBalanceUseCase.getAccountBalance(any(GetAccountBalanceQuery.class)))
				.willReturn(Money.of(2500000L));

		mockMvc.perform(get("/api/v1/wallets/{walletId}/balance", 42L)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.walletId").value(42))
				.andExpect(jsonPath("$.data.currentBalance").value(2500000))
				.andExpect(jsonPath("$.data.currency").value("VND"));
	}
}
