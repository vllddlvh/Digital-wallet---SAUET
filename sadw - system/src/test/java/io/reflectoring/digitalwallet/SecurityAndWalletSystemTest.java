package io.reflectoring.digitalwallet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reflectoring.digitalwallet.adapter.in.web.LinkBankAccountRequest;
import io.reflectoring.digitalwallet.adapter.in.web.LoginRequest;
import io.reflectoring.digitalwallet.adapter.in.web.RegisterRequest;
import io.reflectoring.digitalwallet.adapter.in.web.TransferRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SecurityAndWalletSystemTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void fullWalletLifeCycle_EndToEnd() throws Exception {
		// 1. Đăng ký tài khoản người dùng 1 (Public POST)
		RegisterRequest registerReq1 = new RegisterRequest(
				"user_alice",
				"Password123!",
				"Alice Nguyen",
				"alice@digitalwallet.io"
		);
		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registerReq1)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true));

		// 2. Đăng ký tài khoản người dùng 2 (Public POST)
		RegisterRequest registerReq2 = new RegisterRequest(
				"user_bob",
				"Password123!",
				"Bob Tran",
				"bob@digitalwallet.io"
		);
		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registerReq2)))
				.andExpect(status().isCreated());

		// 2.1 KIỂM THỬ: Đăng ký trùng username -> 400 Bad Request
		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registerReq1)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false));

		// 2.2 KIỂM THỬ: Đăng nhập sai mật khẩu -> 400 Bad Request
		LoginRequest wrongLoginReq = new LoginRequest("user_alice", "WrongPass123!");
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(wrongLoginReq)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false));

		// 3. Đăng nhập để nhận JWT Bearer Token (Public POST)
		LoginRequest loginReq = new LoginRequest("user_alice", "Password123!");
		MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginReq)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();

		JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
		String bearerToken = loginJson.get("data").get("accessToken").asText();
		Long aliceWalletId = loginJson.get("data").get("walletId").asLong();
		assertThat(bearerToken).isNotBlank();

		// 4. KIỂM THỬ BẢO MẬT: Gọi GET /balance KHÔNG có token -> Bị chặn 401 Unauthorized
		mockMvc.perform(get("/api/v1/wallets/{walletId}/balance", aliceWalletId))
				.andExpect(status().isUnauthorized());

		// 5. KIỂM THỬ BẢO MẬT: Gọi GET /balance CÓ token -> 200 OK (Thỏa mãn tiêu chí GET có xác thực)
		mockMvc.perform(get("/api/v1/wallets/{walletId}/balance", aliceWalletId)
				.header("Authorization", "Bearer " + bearerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.walletId").value(aliceWalletId));

		// 6. KIỂM THỬ BẢO MẬT: Gọi POST /transfer KHÔNG có token -> Bị chặn 401 Unauthorized
		TransferRequest transferReq = new TransferRequest(2L, 100L, "Test transfer");
		mockMvc.perform(post("/api/v1/wallets/{sourceWalletId}/transfer", aliceWalletId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(transferReq)))
				.andExpect(status().isUnauthorized());

		// 6.1 KIỂM THỬ BẢO MẬT: Gọi POST /linked-accounts KHÔNG có token -> Bị chặn 401 Unauthorized
		LinkBankAccountRequest linkReq = new LinkBankAccountRequest(
				"VCB",
				"Vietcombank",
				"0123456789",
				"ALICE NGUYEN"
		);
		mockMvc.perform(post("/api/v1/wallets/{walletId}/linked-accounts", aliceWalletId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(linkReq)))
				.andExpect(status().isUnauthorized());

		// 7. Thêm liên kết tài khoản ngân hàng CÓ token (Protected POST)
		MvcResult linkResult = mockMvc.perform(post("/api/v1/wallets/{walletId}/linked-accounts", aliceWalletId)
				.header("Authorization", "Bearer " + bearerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(linkReq)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();

		JsonNode linkJson = objectMapper.readTree(linkResult.getResponse().getContentAsString());
		Long linkedAccountId = linkJson.get("data").get("id").asLong();

		// 7.1 KIỂM THỬ BẢO MẬT: Gọi GET /linked-accounts CÓ token -> 200 OK
		mockMvc.perform(get("/api/v1/wallets/{walletId}/linked-accounts", aliceWalletId)
				.header("Authorization", "Bearer " + bearerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").isArray());

		// 8. KIỂM THỬ BẢO MẬT & PHƯƠNG THỨC DELETE: Gọi DELETE KHÔNG có token -> 401 Unauthorized
		mockMvc.perform(delete("/api/v1/wallets/{walletId}/linked-accounts/{accountId}", aliceWalletId, linkedAccountId))
				.andExpect(status().isUnauthorized());

		// 9. Hủy liên kết tài khoản ngân hàng CÓ token (Protected DELETE - Thỏa mãn tiêu chí DELETE)
		mockMvc.perform(delete("/api/v1/wallets/{walletId}/linked-accounts/{accountId}", aliceWalletId, linkedAccountId)
				.header("Authorization", "Bearer " + bearerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.status").value("UNLINKED"));
	}
}
