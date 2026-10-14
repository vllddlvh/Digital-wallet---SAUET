package io.reflectoring.digitalwallet.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.reflectoring.digitalwallet.application.port.in.LoginUseCase;
import io.reflectoring.digitalwallet.application.port.in.LoginUseCase.LoginCommand;
import io.reflectoring.digitalwallet.application.port.in.LoginUseCase.LoginResult;
import io.reflectoring.digitalwallet.application.port.in.RegisterUserUseCase;
import io.reflectoring.digitalwallet.application.port.in.RegisterUserUseCase.RegisterUserCommand;
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

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private RegisterUserUseCase registerUserUseCase;

	@MockBean
	private LoginUseCase loginUseCase;

	@Test
	void testRegister() throws Exception {
		RegisterRequest request = new RegisterRequest(
				"newuser",
				"Password123!",
				"Nguyen Van A",
				"user@example.com"
		);

		given(registerUserUseCase.registerUser(any(RegisterUserCommand.class))).willReturn(99L);

		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.userId").value(99));
	}

	@Test
	void testLogin() throws Exception {
		LoginRequest request = new LoginRequest("customer_01", "Password123!");

		given(loginUseCase.login(any(LoginCommand.class)))
				.willReturn(new LoginResult(1L, "customer_01", 42L, "mock.jwt.token"));

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.accessToken").value("mock.jwt.token"))
				.andExpect(jsonPath("$.data.userId").value(1))
				.andExpect(jsonPath("$.data.walletId").value(42));
	}

	@Test
	void testRegister_validationFailure() throws Exception {
		RegisterRequest request = new RegisterRequest(
				"", // blank username
				"123", // too short password
				"", // blank fullName
				"invalid-email" // invalid email
		);

		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
	}

	@Test
	void testLogin_validationFailure() throws Exception {
		LoginRequest request = new LoginRequest("", "");

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
	}
}
