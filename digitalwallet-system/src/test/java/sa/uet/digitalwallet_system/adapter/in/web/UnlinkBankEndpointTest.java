package sa.uet.digitalwallet_system.adapter.in.web;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP, JWT, unlink service and persistence-adapter coverage without a live database. */
@WebMvcTest(controllers = LinkedBankController.class, properties = {
        "app.jwt.secret=unlink-bank-endpoint-test-signing-key-2026",
        "app.jwt.expiration-ms=86400000"
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenGenerator.class,
        LinkedBankService.class, LinkedBankPersistenceAdapter.class, GlobalExceptionHandler.class})
class UnlinkBankEndpointTest {

    private static final String ENDPOINT = "/api/v1/linked-banks/501";
    private static final String TEST_SECRET = "unlink-bank-endpoint-test-signing-key-2026";
    private static final long USER_ID = 99L;
    private static final long WALLET_ID = 10L;
    private static final long BANK_ID = 501L;
    private static final LocalDateTime LINKED_AT = LocalDateTime.of(2026, 10, 1, 12, 0);

    @Autowired private WebApplicationContext context;
    @Autowired private JwtTokenGenerator tokenGenerator;
    @MockitoBean private GetWalletUseCase getWalletUseCase;
    @MockitoBean private LinkedBankAccountRepository repository;
    @MockitoBean private LinkedBankAccountMapper mapper;

    private MockMvc mockMvc;

    @BeforeEach
    void createMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void activeAccountIsSoftUnlinkedForAuthenticatedWallet() throws Exception {
        stubWallet(USER_ID, WALLET_ID);
        LinkedBankAccountJpaEntity originalEntity = new LinkedBankAccountJpaEntity();
        originalEntity.setId(BANK_ID);
        LinkedBankAccount original = bank(WALLET_ID, "ACTIVE");
        when(repository.findByIdAndWalletId(BANK_ID, WALLET_ID)).thenReturn(Optional.of(originalEntity));
        when(mapper.toDomain(originalEntity)).thenReturn(original);
        when(mapper.toEntity(any(LinkedBankAccount.class))).thenAnswer(invocation -> {
            LinkedBankAccount changed = invocation.getArgument(0);
            LinkedBankAccountJpaEntity entity = new LinkedBankAccountJpaEntity();
            entity.setId(changed.getId());
            entity.setWalletId(changed.getWalletId());
            entity.setStatus(changed.getStatus());
            entity.setUnlinkedAt(changed.getUnlinkedAt());
            return entity;
        });
        when(repository.save(any(LinkedBankAccountJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        unlinkWithToken(ENDPOINT, tokenFor(USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Bank account unlinked successfully"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.data").doesNotExist());

        var changedCaptor = org.mockito.ArgumentCaptor.forClass(LinkedBankAccount.class);
        verify(mapper).toEntity(changedCaptor.capture());
        LinkedBankAccount changed = changedCaptor.getValue();
        assertEquals(BANK_ID, changed.getId());
        assertEquals(WALLET_ID, changed.getWalletId());
        assertEquals("UNLINKED", changed.getStatus());
        assertEquals(original.getBankCode(), changed.getBankCode());
        assertEquals(original.getBankName(), changed.getBankName());
        assertEquals(original.getAccountNumberMasked(), changed.getAccountNumberMasked());
        assertEquals(original.getHolderName(), changed.getHolderName());
        assertEquals(LINKED_AT, changed.getLinkedAt());
        assertNotNull(changed.getUnlinkedAt());
        assertFalse(changed.getUnlinkedAt().isBefore(LINKED_AT));
        verify(repository).findByIdAndWalletId(BANK_ID, WALLET_ID);
        verify(repository).save(any(LinkedBankAccountJpaEntity.class));
        verify(repository, never()).delete(any(LinkedBankAccountJpaEntity.class));
    }

    @Test
    void anotherUserCannotUnlinkAccountFromFirstWallet() throws Exception {
        stubWallet(100L, 20L);
        when(repository.findByIdAndWalletId(BANK_ID, 20L)).thenReturn(Optional.empty());

        unlinkWithToken(ENDPOINT, tokenFor(100L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Linked bank account not found"));
        verify(repository).findByIdAndWalletId(BANK_ID, 20L);
        verify(repository, never()).findByIdAndWalletId(BANK_ID, WALLET_ID);
        verify(repository, never()).save(any());
    }

    @Test
    void nonexistentBankIdReturns404WithoutSave() throws Exception {
        stubWallet(USER_ID, WALLET_ID);
        when(repository.findByIdAndWalletId(BANK_ID, WALLET_ID)).thenReturn(Optional.empty());

        unlinkWithToken(ENDPOINT, tokenFor(USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value(ENDPOINT));
        verify(repository, never()).save(any());
    }

    @Test
    void alreadyUnlinkedAccountReturns404WithoutSecondSave() throws Exception {
        stubWallet(USER_ID, WALLET_ID);
        LinkedBankAccountJpaEntity entity = new LinkedBankAccountJpaEntity();
        entity.setId(BANK_ID);
        when(repository.findByIdAndWalletId(BANK_ID, WALLET_ID)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(bank(WALLET_ID, "UNLINKED"));

        unlinkWithToken(ENDPOINT, tokenFor(USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Linked bank account not found"));
        verify(repository, never()).save(any());
    }

    @Test
    void missingTokenReturns401BeforeWalletLookup() throws Exception {
        mockMvc.perform(delete(ENDPOINT))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verifyNoInteractions(getWalletUseCase, repository, mapper);
    }

    @Test
    void invalidTokenReturns401BeforeWalletLookup() throws Exception {
        unlinkWithToken(ENDPOINT, "invalid.jwt.token")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verifyNoInteractions(getWalletUseCase, repository, mapper);
    }

    @Test
    void expiredTokenReturns401BeforeWalletLookup() throws Exception {
        String expired = Jwts.builder().subject(String.valueOf(USER_ID)).claim("role", "ROLE_USER")
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        unlinkWithToken(ENDPOINT, expired)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verifyNoInteractions(getWalletUseCase, repository, mapper);
    }

    @Test
    void missingWalletReturns404WithoutRepositoryAccess() throws Exception {
        when(getWalletUseCase.getWallet(USER_ID)).thenThrow(new WalletNotFoundException());

        unlinkWithToken(ENDPOINT, tokenFor(USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Wallet not found"));
        verifyNoInteractions(repository, mapper);
    }

    @Test
    void nonNumericIdReturns400WithoutRepositoryAccess() throws Exception {
        int actualStatus = unlinkWithToken("/api/v1/linked-banks/not-a-number", tokenFor(USER_ID))
                .andReturn().getResponse().getStatus();
        verifyNoInteractions(getWalletUseCase, repository, mapper);
        assertEquals(400, actualStatus);
    }

    @Test
    void repositoryFailureReturns500() throws Exception {
        stubWallet(USER_ID, WALLET_ID);
        when(repository.findByIdAndWalletId(BANK_ID, WALLET_ID))
                .thenThrow(new IllegalStateException("Storage unavailable"));

        unlinkWithToken(ENDPOINT, tokenFor(USER_ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.path").value(ENDPOINT));
    }

    private ResultActions unlinkWithToken(String path, String token) throws Exception {
        return mockMvc.perform(delete(path).header("Authorization", "Bearer " + token));
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

    private LinkedBankAccount bank(long walletId, String status) {
        return LinkedBankAccount.builder().id(BANK_ID).walletId(walletId).bankCode("VCB")
                .bankName("Example Bank").accountNumberMasked("******6789")
                .holderName("NGUYEN VAN A").status(status).linkedAt(LINKED_AT).build();
    }
}
