package sa.uet.digitalwallet_system.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import sa.uet.digitalwallet_system.adapter.in.security.JwtAuthenticationFilter;
import sa.uet.digitalwallet_system.adapter.in.security.SecurityConfig;
import sa.uet.digitalwallet_system.adapter.in.web.controller.LinkedBankController;
import sa.uet.digitalwallet_system.adapter.in.web.handler.GlobalExceptionHandler;
import sa.uet.digitalwallet_system.adapter.out.security.JwtTokenGenerator;
import sa.uet.digitalwallet_system.application.port.in.GetWalletUseCase;
import sa.uet.digitalwallet_system.application.port.out.LoadLinkedBankPort;
import sa.uet.digitalwallet_system.application.port.out.SaveLinkedBankPort;
import sa.uet.digitalwallet_system.application.service.LinkedBankService;
import sa.uet.digitalwallet_system.domain.exception.WalletNotFoundException;
import sa.uet.digitalwallet_system.domain.model.LinkedBankAccount;
import sa.uet.digitalwallet_system.domain.model.User;
import sa.uet.digitalwallet_system.domain.model.Wallet;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests the POST endpoint with the real security filters, validation and bank-link
 * service. Only wallet lookup and persistence ports are mocked; no database is used.
 */
@WebMvcTest(controllers = LinkedBankController.class, properties = {
        "app.jwt.secret=link-bank-endpoint-test-only-signing-key-2026",
        "app.jwt.expiration-ms=86400000"
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenGenerator.class,
        LinkedBankService.class, GlobalExceptionHandler.class})
class LinkBankEndpointTest {

    private static final String ENDPOINT = "/api/v1/linked-banks";
    private static final long USER_ID = 99L;
    private static final long WALLET_ID = 10L;
    private static final String TEST_SECRET = "link-bank-endpoint-test-only-signing-key-2026";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenGenerator tokenGenerator;

    @MockitoBean
    private GetWalletUseCase getWalletUseCase;

    @MockitoBean
    private SaveLinkedBankPort saveLinkedBankPort;

    @MockitoBean
    private LoadLinkedBankPort loadLinkedBankPort;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String validToken;

    @BeforeEach
    void createToken() {
        // Register JWT through the real SecurityFilterChain only. Registering the
        // same OncePerRequestFilter as an outer MockMvc filter can clear its context.
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        validToken = tokenGenerator.generateToken(User.builder()
                .id(USER_ID).username("bank-link-user").role("ROLE_USER").build());
    }

    @Test
    void validRequestReturns201AndLinksMaskedAccountToAuthenticatedWallet() throws Exception {
        stubWallet();
        stubSuccessfulSave();
        LocalDateTime beforeRequest = LocalDateTime.now();

        postJson(validRequest())
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.message").value("Bank account linked successfully"))
                .andExpect(jsonPath("$.data.id").value(501))
                .andExpect(jsonPath("$.data.walletId").value(WALLET_ID))
                .andExpect(jsonPath("$.data.bankCode").value("VCB"))
                .andExpect(jsonPath("$.data.bankName").value("Vietcombank"))
                .andExpect(jsonPath("$.data.accountNumberMasked").value("******6789"))
                .andExpect(jsonPath("$.data.holderName").value("NGUYEN VAN A"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.linkedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.unlinkedAt").isEmpty())
                .andExpect(jsonPath("$.data.accountNumber").doesNotExist())
                .andExpect(content().string(not(containsString("0123456789"))));

        ArgumentCaptor<LinkedBankAccount> saved = ArgumentCaptor.forClass(LinkedBankAccount.class);
        verify(getWalletUseCase).getWallet(USER_ID);
        verify(saveLinkedBankPort, times(1)).save(saved.capture());
        assertAll(
                () -> assertEquals(WALLET_ID, saved.getValue().getWalletId()),
                () -> assertEquals("******6789", saved.getValue().getAccountNumberMasked()),
                () -> assertEquals("ACTIVE", saved.getValue().getStatus()),
                () -> assertNull(saved.getValue().getUnlinkedAt()),
                () -> assertFalse(saved.getValue().getLinkedAt().isBefore(beforeRequest)),
                () -> assertFalse(saved.getValue().getLinkedAt().isAfter(LocalDateTime.now()))
        );
        verifyNoInteractions(loadLinkedBankPort);
    }

    @Test
    void fieldsAtMaximumLengthsReturn201() throws Exception {
        stubWallet();
        stubSuccessfulSave();
        Map<String, String> request = validRequest();
        request.put("bankCode", "B".repeat(20));
        request.put("bankName", "N".repeat(100));
        request.put("accountNumber", "1".repeat(46) + "6789");
        request.put("holderName", "H".repeat(100));

        postJson(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bankCode").value(request.get("bankCode")))
                .andExpect(jsonPath("$.data.bankName").value(request.get("bankName")))
                .andExpect(jsonPath("$.data.holderName").value(request.get("holderName")))
                .andExpect(jsonPath("$.data.accountNumberMasked").value("******6789"));
        verify(saveLinkedBankPort, times(1)).save(any(LinkedBankAccount.class));
    }

    @ParameterizedTest(name = "{index}: {0} is {2}")
    @MethodSource("invalidFields")
    void invalidFieldReturns400WithoutCallingBusinessLogic(String field, String value, String variant)
            throws Exception {
        Map<String, String> request = validRequest();
        if ("omitted".equals(variant)) {
            request.remove(field);
        } else {
            request.put(field, value);
        }

        postJson(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value(ENDPOINT))
                .andExpect(jsonPath("$.errors." + field).isNotEmpty());
        verifyNoInteractions(getWalletUseCase, saveLinkedBankPort, loadLinkedBankPort);
    }

    static Stream<Arguments> invalidFields() {
        Map<String, Integer> limits = new LinkedHashMap<>();
        limits.put("bankCode", 20);
        limits.put("bankName", 100);
        limits.put("accountNumber", 50);
        limits.put("holderName", 100);
        return limits.entrySet().stream().flatMap(entry -> Stream.of(
                Arguments.of(entry.getKey(), null, "omitted"),
                Arguments.of(entry.getKey(), null, "null"),
                Arguments.of(entry.getKey(), "", "empty"),
                Arguments.of(entry.getKey(), "   ", "whitespace"),
                Arguments.of(entry.getKey(), "X".repeat(entry.getValue() + 1), "too-long")
        ));
    }

    @Test
    void missingTokenReturns401() throws Exception {
        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
        verifyNoInteractions(getWalletUseCase, saveLinkedBankPort, loadLinkedBankPort);
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        postWithToken("invalid.jwt.token")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
        verifyNoInteractions(getWalletUseCase, saveLinkedBankPort, loadLinkedBankPort);
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        String expired = Jwts.builder().subject(String.valueOf(USER_ID)).claim("role", "ROLE_USER")
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        postWithToken(expired)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verifyNoInteractions(getWalletUseCase, saveLinkedBankPort, loadLinkedBankPort);
    }

    @Test
    void missingWalletReturns404WithoutSavingBankAccount() throws Exception {
        when(getWalletUseCase.getWallet(USER_ID)).thenThrow(new WalletNotFoundException());

        postJson(validRequest())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Wallet not found"))
                .andExpect(jsonPath("$.path").value(ENDPOINT));
        verifyNoInteractions(saveLinkedBankPort, loadLinkedBankPort);
    }

    @Test
    void storageFailureReturns500() throws Exception {
        stubWallet();
        when(saveLinkedBankPort.save(any(LinkedBankAccount.class)))
                .thenThrow(new IllegalStateException("Storage unavailable"));

        postJson(validRequest())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.path").value(ENDPOINT));
    }

    @Test
    void missingBodyReturns400WithoutCallingBusinessLogic() throws Exception {
        ResultActions response = mockMvc.perform(post(ENDPOINT)
                .header("Authorization", "Bearer " + validToken).contentType(MediaType.APPLICATION_JSON));
        verifyNoInteractions(getWalletUseCase, saveLinkedBankPort, loadLinkedBankPort);
        response.andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonReturns400WithoutCallingBusinessLogic() throws Exception {
        ResultActions response = mockMvc.perform(post(ENDPOINT)
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"bankCode\": "));
        verifyNoInteractions(getWalletUseCase, saveLinkedBankPort, loadLinkedBankPort);
        response.andExpect(status().isBadRequest());
    }

    private ResultActions postJson(Map<String, String> request) throws Exception {
        return mockMvc.perform(post(ENDPOINT).header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions postWithToken(String token) throws Exception {
        return mockMvc.perform(post(ENDPOINT).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(validRequest())));
    }

    private Map<String, String> validRequest() {
        Map<String, String> request = new LinkedHashMap<>();
        request.put("bankCode", "VCB");
        request.put("bankName", "Vietcombank");
        request.put("accountNumber", "0123456789");
        request.put("holderName", "NGUYEN VAN A");
        return request;
    }

    private void stubWallet() {
        when(getWalletUseCase.getWallet(USER_ID)).thenReturn(Wallet.builder()
                .id(WALLET_ID).userId(USER_ID).walletNumber("WAL0000000010")
                .balance(0L).status("ACTIVE").build());
    }

    private void stubSuccessfulSave() {
        when(saveLinkedBankPort.save(any(LinkedBankAccount.class))).thenAnswer(invocation -> {
            LinkedBankAccount account = invocation.getArgument(0);
            return LinkedBankAccount.builder().id(501L).walletId(account.getWalletId())
                    .bankCode(account.getBankCode()).bankName(account.getBankName())
                    .accountNumberMasked(account.getAccountNumberMasked()).holderName(account.getHolderName())
                    .status(account.getStatus()).linkedAt(account.getLinkedAt())
                    .unlinkedAt(account.getUnlinkedAt()).build();
        });
    }
}
