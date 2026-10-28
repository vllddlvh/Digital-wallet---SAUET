package sa.uet.digitalwallet_system.adapter.in.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import sa.uet.digitalwallet_system.adapter.in.security.JwtAuthenticationFilter;
import sa.uet.digitalwallet_system.adapter.in.security.SecurityConfig;
import sa.uet.digitalwallet_system.adapter.in.web.controller.TransactionController;
import sa.uet.digitalwallet_system.adapter.in.web.handler.GlobalExceptionHandler;
import sa.uet.digitalwallet_system.adapter.out.persistence.TransactionPersistenceAdapter;
import sa.uet.digitalwallet_system.adapter.out.persistence.entity.TransactionJpaEntity;
import sa.uet.digitalwallet_system.adapter.out.persistence.mapper.TransactionEntityMapper;
import sa.uet.digitalwallet_system.adapter.out.persistence.repository.TransactionRepository;
import sa.uet.digitalwallet_system.adapter.out.security.JwtTokenGenerator;
import sa.uet.digitalwallet_system.application.port.in.GetWalletUseCase;
import sa.uet.digitalwallet_system.application.port.out.*;
import sa.uet.digitalwallet_system.application.service.TransactionService;
import sa.uet.digitalwallet_system.domain.exception.WalletNotFoundException;
import sa.uet.digitalwallet_system.domain.model.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TransactionController.class, properties = {
        "app.jwt.secret=get-transactions-test-signing-key-2026", "app.jwt.expiration-ms=86400000"
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenGenerator.class,
        TransactionService.class, TransactionPersistenceAdapter.class, GlobalExceptionHandler.class})
class GetTransactionsEndpointTest {
    private static final String PATH = "/api/v1/transactions";
    @Autowired WebApplicationContext context;
    @Autowired JwtTokenGenerator tokenGenerator;
    @MockitoBean GetWalletUseCase getWalletUseCase;
    @MockitoBean LoadWalletPort loadWalletPort;
    @MockitoBean SaveWalletPort saveWalletPort;
    @MockitoBean LoadLinkedBankPort loadLinkedBankPort;
    @MockitoBean TransactionRepository repository;
    @MockitoBean TransactionEntityMapper mapper;
    MockMvc mvc;

    @BeforeEach void setUp() { mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }

    @Test void returnsTransactionsForAuthenticatedWallet() throws Exception {
        stubWallet(99, 10);
        TransactionJpaEntity first = new TransactionJpaEntity(); first.setId(101L);
        TransactionJpaEntity second = new TransactionJpaEntity(); second.setId(100L);
        when(repository.findByWalletId(10L)).thenReturn(List.of(first, second));
        when(mapper.toDomain(first)).thenReturn(Transaction.builder().id(101L).type("TOPUP")
                .targetWalletId(10L).amount(250L).referenceCode("ref-101")
                .createdAt(LocalDateTime.of(2026, 10, 28, 12, 0)).build());
        when(mapper.toDomain(second)).thenReturn(Transaction.builder().id(100L).type("TRANSFER")
                .sourceWalletId(10L).targetWalletId(20L).amount(100L).referenceCode("ref-100")
                .createdAt(LocalDateTime.of(2026, 10, 27, 12, 0)).build());

        request(token(99)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(101))
                .andExpect(jsonPath("$.data[0].type").value("TOPUP"))
                .andExpect(jsonPath("$.data[1].id").value(100))
                .andExpect(jsonPath("$.data[1].type").value("TRANSFER"));
        verify(repository).findByWalletId(10L);
        verify(repository, never()).save(any());
    }

    @Test void emptyHistoryReturnsEmptyArray() throws Exception {
        stubWallet(99, 10);
        when(repository.findByWalletId(10L)).thenReturn(List.of());
        request(token(99)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test void secondUserQueriesOnlyOwnWallet() throws Exception {
        stubWallet(100, 20);
        when(repository.findByWalletId(20L)).thenReturn(List.of());
        request(token(100)).andExpect(status().isOk());
        verify(repository).findByWalletId(20L);
        verify(repository, never()).findByWalletId(10L);
    }

    @Test void missingTokenReturns401WithoutQuery() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        verifyNoInteractions(getWalletUseCase, repository, mapper);
    }

    @Test void invalidTokenReturns401WithoutQuery() throws Exception {
        request("invalid.jwt.token").andExpect(status().isUnauthorized());
        verifyNoInteractions(getWalletUseCase, repository, mapper);
    }

    @Test void missingWalletReturns404WithoutQuery() throws Exception {
        when(getWalletUseCase.getWallet(99L)).thenThrow(new WalletNotFoundException());
        request(token(99)).andExpect(status().isNotFound());
        verifyNoInteractions(repository, mapper);
    }

    @Test void repositoryFailureReturns500() throws Exception {
        stubWallet(99, 10);
        when(repository.findByWalletId(10L)).thenThrow(new IllegalStateException("Storage unavailable"));
        request(token(99)).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    private org.springframework.test.web.servlet.ResultActions request(String jwt) throws Exception {
        return mvc.perform(get(PATH).header("Authorization", "Bearer " + jwt));
    }

    private String token(long userId) { return tokenGenerator.generateToken(User.builder().id(userId)
            .username("history-user-" + userId).role("ROLE_USER").build()); }

    private void stubWallet(long userId, long walletId) {
        when(getWalletUseCase.getWallet(userId)).thenReturn(Wallet.builder().id(walletId)
                .userId(userId).walletNumber("WAL" + walletId).balance(0L).status("ACTIVE").build());
    }
}
