package sa.uet.digitalwallet_system.adapter.in.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sa.uet.digitalwallet_system.adapter.in.web.dto.request.LoginRequest;
import sa.uet.digitalwallet_system.adapter.in.web.dto.request.RegisterRequest;
import sa.uet.digitalwallet_system.adapter.out.security.JwtTokenGenerator;
import sa.uet.digitalwallet_system.application.port.in.GetWalletUseCase;
import sa.uet.digitalwallet_system.application.port.in.LoginCommand;
import sa.uet.digitalwallet_system.application.port.in.LoginUseCase;
import sa.uet.digitalwallet_system.application.port.in.RegisterCommand;
import sa.uet.digitalwallet_system.application.port.in.RegisterUseCase;
import sa.uet.digitalwallet_system.domain.model.User;
import sa.uet.digitalwallet_system.domain.model.Wallet;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JwtTokenGenerator jwtTokenGenerator;

    @Autowired(required = false)
    private UserDetailsService userDetailsService;

    @Autowired(required = false)
    private InMemoryUserDetailsManager inMemoryUserDetailsManager;

    @MockitoBean
    private GetWalletUseCase getWalletUseCase;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private RegisterUseCase registerUseCase;

    @Test
    @DisplayName("UserDetailsService and InMemoryUserDetailsManager beans must not be created")
    void defaultUserDetailsServiceBeans_ShouldNotExist() {
        assertNull(userDetailsService, "UserDetailsService bean must not exist");
        assertNull(inMemoryUserDetailsManager, "InMemoryUserDetailsManager bean must not exist");
    }

    @Test
    @DisplayName("Protected endpoint without token should return 401 Unauthorized")
    void protectedEndpoint_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @DisplayName("Protected endpoint with invalid JWT should return 401 Unauthorized")
    void protectedEndpoint_WithInvalidJwt_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/me")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @DisplayName("Protected endpoint with valid JWT should return 200 OK")
    void protectedEndpoint_WithValidJwt_Returns200() throws Exception {
        User user = User.builder()
                .id(99L)
                .username("testuser")
                .role("ROLE_USER")
                .build();
        String validToken = jwtTokenGenerator.generateToken(user);

        Wallet mockWallet = Wallet.builder()
                .id(1L)
                .userId(99L)
                .walletNumber("W123456789")
                .balance(500000L)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        when(getWalletUseCase.getWallet(99L)).thenReturn(mockWallet);

        mockMvc.perform(get("/api/v1/wallets/me")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.walletNumber").value("W123456789"))
                .andExpect(jsonPath("$.data.balance").value(500000));
    }

    @Test
    @DisplayName("Public endpoint POST /api/v1/auth/login is accessible without JWT")
    void publicLoginEndpoint_IsAccessibleWithoutToken() throws Exception {
        when(loginUseCase.login(any(LoginCommand.class))).thenReturn("mocked.jwt.token");

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("mocked.jwt.token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("Public endpoint POST /api/v1/auth/register is accessible without JWT")
    void publicRegisterEndpoint_IsAccessibleWithoutToken() throws Exception {
        User registeredUser = User.builder()
                .id(1L)
                .username("newuser")
                .fullName("New User")
                .email("new@example.com")
                .role("ROLE_USER")
                .createdAt(LocalDateTime.now())
                .build();

        when(registerUseCase.register(any(RegisterCommand.class))).thenReturn(registeredUser);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setFullName("New User");
        request.setEmail("new@example.com");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.username").value("newuser"));
    }

    @Test
    @DisplayName("OpenAPI / Swagger endpoint is accessible without JWT")
    void swaggerEndpoints_AreAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("openapi")));
    }
}
