package io.reflectoring.digitalwallet.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.reflectoring.digitalwallet.application.port.in.SendMoneyCommand;
import io.reflectoring.digitalwallet.application.port.in.SendMoneyUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WalletTransferController.class)
@AutoConfigureMockMvc(addFilters = false)
class WalletTransferControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private SendMoneyUseCase sendMoneyUseCase;

	@Test
	void testTransferSuccess() throws Exception {
		TransferRequest request = new TransferRequest(2L, 500000L, "Tien an trua");

		given(sendMoneyUseCase.sendMoney(any(SendMoneyCommand.class))).willReturn(true);

		mockMvc.perform(post("/api/v1/wallets/{sourceWalletId}/transfer", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.sourceWalletId").value(1))
				.andExpect(jsonPath("$.data.targetWalletId").value(2))
				.andExpect(jsonPath("$.data.amount").value(500000))
				.andExpect(jsonPath("$.data.status").value("SUCCESS"));
	}

	@Test
	void testTransferFailure_insufficientBalance() throws Exception {
		TransferRequest request = new TransferRequest(2L, 500000L, "Tien an trua");

		given(sendMoneyUseCase.sendMoney(any(SendMoneyCommand.class))).willReturn(false);

		mockMvc.perform(post("/api/v1/wallets/{sourceWalletId}/transfer", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.errorCode").value("TRANSFER_FAILED"));
	}

	@Test
	void testTransferFailure_invalidAmount() throws Exception {
		TransferRequest request = new TransferRequest(2L, -5000L, "Negative amount");

		mockMvc.perform(post("/api/v1/wallets/{sourceWalletId}/transfer", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
	}

	@Test
	void testTransferFailure_nullTargetWallet() throws Exception {
		TransferRequest request = new TransferRequest(null, 50000L, "No target");

		mockMvc.perform(post("/api/v1/wallets/{sourceWalletId}/transfer", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
	}
}
