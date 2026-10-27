package sa.uet.digitalwallet_system.adapter.in.web;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import sa.uet.digitalwallet_system.adapter.out.persistence.LinkedBankPersistenceAdapter;
import sa.uet.digitalwallet_system.adapter.out.persistence.entity.LinkedBankAccountJpaEntity;
import sa.uet.digitalwallet_system.adapter.out.persistence.mapper.LinkedBankAccountMapper;
import sa.uet.digitalwallet_system.adapter.out.persistence.repository.LinkedBankAccountRepository;
import sa.uet.digitalwallet_system.adapter.out.security.JwtTokenGenerator;
import sa.uet.digitalwallet_system.application.port.in.GetWalletUseCase;
import sa.uet.digitalwallet_system.application.service.LinkedBankService;
import sa.uet.digitalwallet_system.domain.exception.WalletNotFoundException;
import sa.uet.digitalwallet_system.domain.model.LinkedBankAccount;
import sa.uet.digitalwallet_system.domain.model.User;
import sa.uet.digitalwallet_system.domain.model.Wallet;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP, JWT, service and persistence-adapter coverage without a live database. */
@WebMvcTest(controllers = LinkedBankController.class, properties = {
        "app.jwt.secret=get-linked-banks-endpoint-test-signing-key-2026",
        "app.jwt.expiration-ms=86400000"
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenGenerator.class,
        LinkedBankService.class, LinkedBankPersistenceAdapter.class, GlobalExceptionHandler.class})
class GetLinkedBanksEndpointTest {

    private static final String ENDPOINT = "/api/v1/linked-banks";
    private static final String TEST_SECRET = "get-linked-banks-endpoint-test-signing-key-2026";
    private static final long USER_ID = 99L;
    private static final long WALLET_ID = 10L;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenGenerator tokenGenerator;

    @MockitoBean
    private GetWalletUseCase getWalletUseCase;

    @MockitoBean
    private LinkedBankAccountRepository repository;

    @MockitoBean
    private LinkedBankAccountMapper mapper;

    private MockMvc mockMvc;

    @BeforeEach
    void createMvc() {
        // Run the JWT filter inside the real SecurityFilterChain exactly once.
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void returnsActiveAccountsFromAuthenticatedWalletAsMaskedArray() throws Exception {
        stubWallet(USER_ID, WALLET_ID);
        LinkedBankAccountJpaEntity firstEntity = new LinkedBankAccountJpaEntity();
        LinkedBankAccountJpaEntity secondEntity = new LinkedBankAccountJpaEntity();
        firstEntity.setId(501L);
        secondEntity.setId(502L);
        when(repository.findByWalletIdAndStatus(WALLET_ID, "ACTIVE"))
                .thenReturn(List.of(firstEntity, secondEntity));
        when(mapper.toDomain(firstEntity)).thenReturn(bank(501L, WALLET_ID, "VCB", "******6789"));
        when(mapper.toDomain(secondEntity)).thenReturn(bank(502L, WALLET_ID, "TCB", "******4321"));

        getWithToken(tokenFor(USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[*].id", containsInAnyOrder(501, 502)))
                .andExpect(jsonPath("$.data[0].walletId").value(WALLET_ID))
                .andExpect(jsonPath("$.data[0].accountNumberMasked").value("******6789"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data[0].linkedAt").isNotEmpty())
                .andExpect(jsonPath("$.data[0].accountNumber").doesNotExist())
                .andExpect(content().string(not(containsString("0123456789"))));

        verify(getWalletUseCase).getWallet(USER_ID);
        verify(repository).findByWalletIdAndStatus(WALLET_ID, "ACTIVE");
        verify(repository, never()).save(any());
    }

    @Test
    void returnsEmptyArrayWhenWalletHasNoActiveLinkedAccounts() throws Exception {
        stubWallet(USER_ID, WALLET_ID);
        when(repository.findByWalletIdAndStatus(WALLET_ID, "ACTIVE")).thenReturn(List.of());

        getWithToken(tokenFor(USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
        verify(repository).findByWalletIdAndStatus(WALLET_ID, "ACTIVE");
        verifyNoInteractions(mapper);
    }

    @Test
    void aSecondUserQueriesOnlyThatUsersWallet() throws Exception {
        stubWallet(100L, 20L);
        when(repository.findByWalletIdAndStatus(20L, "ACTIVE")).thenReturn(List.of());

        getWithToken(tokenFor(100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
        verify(getWalletUseCase).getWallet(100L);
        verify(repository).findByWalletIdAndStatus(20L, "ACTIVE");
        verify(repository, never()).findByWalletIdAndStatus(WALLET_ID, "ACTIVE");
    }

    @Test
    void missingTokenReturns401BeforeWalletLookup() throws Exception {
        mockMvc.perform(get(ENDPOINT))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
        verifyNoInteractions(getWalletUseCase, repository, mapper);
    }

    @Test
    void invalidTokenReturns401BeforeWalletLookup() throws Exception {
        getWithToken("invalid.jwt.token")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
        verifyNoInteractions(getWalletUseCase, repository, mapper);
    }

    @Test
    void expiredTokenReturns401BeforeWalletLookup() throws Exception {
        String expired = Jwts.builder().subject(String.valueOf(USER_ID)).claim("role", "ROLE_USER")
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        getWithToken(expired)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verifyNoInteractions(getWalletUseCase, repository, mapper);
    }

    @Test
    void missingWalletReturns404WithoutRepositoryQuery() throws Exception {
        when(getWalletUseCase.getWallet(USER_ID)).thenThrow(new WalletNotFoundException());

        getWithToken(tokenFor(USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Wallet not found"))
                .andExpect(jsonPath("$.path").value(ENDPOINT));
        verifyNoInteractions(repository, mapper);
    }

    @Test
    void repositoryFailureReturns500() throws Exception {
        stubWallet(USER_ID, WALLET_ID);
        when(repository.findByWalletIdAndStatus(WALLET_ID, "ACTIVE"))
                .thenThrow(new IllegalStateException("Storage unavailable"));

        getWithToken(tokenFor(USER_ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.path").value(ENDPOINT));
    }

    private ResultActions getWithToken(String token) throws Exception {
        return mockMvc.perform(get(ENDPOINT).header("Authorization", "Bearer " + token));
    }

    private String tokenFor(long userId) {
        return tokenGenerator.generateToken(User.builder()
                .id(userId).username("bank-user-" + userId).role("ROLE_USER").build());
    }

    private void stubWallet(long userId, long walletId) {
        when(getWalletUseCase.getWallet(userId)).thenReturn(Wallet.builder()
                .id(walletId).userId(userId).walletNumber("WAL" + walletId)
                .balance(0L).status("ACTIVE").build());
    }

    private LinkedBankAccount bank(long id, long walletId, String bankCode, String maskedNumber) {
        return LinkedBankAccount.builder().id(id).walletId(walletId).bankCode(bankCode)
                .bankName("Example Bank").accountNumberMasked(maskedNumber)
                .holderName("NGUYEN VAN A").status("ACTIVE")
                .linkedAt(LocalDateTime.of(2026, 10, 28, 0, 0)).build();
    }
}
