package sa.uet.digitalwallet_system.adapter.in.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import sa.uet.digitalwallet_system.adapter.in.security.JwtAuthenticationFilter;
import sa.uet.digitalwallet_system.adapter.in.security.SecurityConfig;
import sa.uet.digitalwallet_system.adapter.in.web.controller.TransactionController;
import sa.uet.digitalwallet_system.adapter.in.web.handler.GlobalExceptionHandler;
import sa.uet.digitalwallet_system.adapter.out.security.JwtTokenGenerator;
import sa.uet.digitalwallet_system.application.port.in.GetWalletUseCase;
import sa.uet.digitalwallet_system.application.port.out.*;
import sa.uet.digitalwallet_system.application.service.TransactionService;
import sa.uet.digitalwallet_system.domain.exception.WalletNotFoundException;
import sa.uet.digitalwallet_system.domain.model.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TransactionController.class, properties = {
        "app.jwt.secret=topup-endpoint-test-signing-key-2026", "app.jwt.expiration-ms=86400000"
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenGenerator.class,
        TransactionService.class, GlobalExceptionHandler.class})
class TopupEndpointTest {
    private static final String PATH = "/api/v1/transactions/topup";
    private static final String BODY = "{\"linkedBankAccountId\":501,\"amount\":250,\"description\":\"Nap tien\"}";
    @Autowired WebApplicationContext context;
    @Autowired JwtTokenGenerator tokenGenerator;
    @MockitoBean GetWalletUseCase getWalletUseCase;
    @MockitoBean LoadWalletPort loadWalletPort;
    @MockitoBean SaveWalletPort saveWalletPort;
    @MockitoBean LoadLinkedBankPort loadLinkedBankPort;
    @MockitoBean SaveTransactionPort saveTransactionPort;
    @MockitoBean LoadTransactionPort loadTransactionPort;
    MockMvc mvc;

    @BeforeEach void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test void successfulTopupCreditsWalletAndCreatesLedgerEntry() throws Exception {
        stub("ACTIVE", "ACTIVE");
        request(BODY, token()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Topup successful"))
                .andExpect(jsonPath("$.data.type").value("TOPUP"))
                .andExpect(jsonPath("$.data.targetWalletId").value(10))
                .andExpect(jsonPath("$.data.linkedBankAccountId").value(501))
                .andExpect(jsonPath("$.data.amount").value(250))
                .andExpect(jsonPath("$.data.sourceWalletId").doesNotExist())
                .andExpect(jsonPath("$.data.referenceCode").isNotEmpty());
        var wallet = org.mockito.ArgumentCaptor.forClass(Wallet.class);
        verify(saveWalletPort).save(wallet.capture());
        assertEquals(1250L, wallet.getValue().getBalance());
        var tx = org.mockito.ArgumentCaptor.forClass(Transaction.class);
        verify(saveTransactionPort).save(tx.capture());
        assertEquals("TOPUP", tx.getValue().getType());
        assertEquals(250L, tx.getValue().getAmount());
        verify(loadLinkedBankPort).loadByIdAndWalletId(501L, 10L);
    }

    @Test void missingBankIdReturns400() throws Exception {
        request("{\"amount\":250}", token()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.linkedBankAccountId").isNotEmpty());
        noBusinessCalls();
    }

    @Test void missingAmountReturns400() throws Exception {
        request("{\"linkedBankAccountId\":501}", token()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").isNotEmpty());
        noBusinessCalls();
    }

    @Test void zeroAmountReturns400() throws Exception {
        request("{\"linkedBankAccountId\":501,\"amount\":0}", token())
                .andExpect(status().isBadRequest());
        noBusinessCalls();
    }

    @Test void negativeAmountReturns400() throws Exception {
        request("{\"linkedBankAccountId\":501,\"amount\":-1}", token())
                .andExpect(status().isBadRequest());
        noBusinessCalls();
    }

    @Test void tooLongDescriptionReturns400() throws Exception {
        String body = "{\"linkedBankAccountId\":501,\"amount\":250,\"description\":\"" + "a".repeat(256) + "\"}";
        request(body, token()).andExpect(status().isBadRequest());
        noBusinessCalls();
    }

    @Test void missingTokenReturns401() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        noBusinessCalls();
    }

    @Test void invalidTokenReturns401() throws Exception {
        request(BODY, "invalid.jwt.token").andExpect(status().isUnauthorized());
        noBusinessCalls();
    }

    @Test void missingWalletReturns404WithoutWrites() throws Exception {
        when(getWalletUseCase.getWallet(99L)).thenThrow(new WalletNotFoundException());
        request(BODY, token()).andExpect(status().isNotFound());
        verifyNoInteractions(loadWalletPort, saveWalletPort, loadLinkedBankPort, saveTransactionPort);
    }

    @Test void inactiveWalletReturns400WithoutWrites() throws Exception {
        when(getWalletUseCase.getWallet(99L)).thenReturn(wallet("INACTIVE"));
        when(loadWalletPort.loadById(10L)).thenReturn(Optional.of(wallet("INACTIVE")));
        request(BODY, token()).andExpect(status().isBadRequest());
        verifyNoInteractions(saveWalletPort, loadLinkedBankPort, saveTransactionPort);
    }

    @Test void foreignBankReturns404WithoutWrites() throws Exception {
        when(getWalletUseCase.getWallet(99L)).thenReturn(wallet("ACTIVE"));
        when(loadWalletPort.loadById(10L)).thenReturn(Optional.of(wallet("ACTIVE")));
        when(loadLinkedBankPort.loadByIdAndWalletId(501L, 10L)).thenReturn(Optional.empty());
        request(BODY, token()).andExpect(status().isNotFound());
        verifyNoInteractions(saveWalletPort, saveTransactionPort);
    }

    @Test void unlinkedBankMustNotBeUsableForTopup() throws Exception {
        stub("ACTIVE", "UNLINKED");
        int actual = request(BODY, token()).andReturn().getResponse().getStatus();
        assertEquals(400, actual);
        verifyNoInteractions(saveWalletPort, saveTransactionPort);
    }

    private org.springframework.test.web.servlet.ResultActions request(String body, String jwt) throws Exception {
        return mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwt).content(body));
    }

    private String token() {
        return tokenGenerator.generateToken(User.builder().id(99L).username("topup-user")
                .role("ROLE_USER").build());
    }

    private Wallet wallet(String status) {
        return Wallet.builder().id(10L).userId(99L).walletNumber("WAL10")
                .balance(1000L).status(status).build();
    }

    private void stub(String walletStatus, String bankStatus) {
        when(getWalletUseCase.getWallet(99L)).thenReturn(wallet(walletStatus));
        when(loadWalletPort.loadById(10L)).thenReturn(Optional.of(wallet(walletStatus)));
        when(loadLinkedBankPort.loadByIdAndWalletId(501L, 10L)).thenReturn(Optional.of(
                LinkedBankAccount.builder().id(501L).walletId(10L).status(bankStatus).build()));
        when(saveTransactionPort.save(any(Transaction.class)))
                .thenAnswer(call -> call.getArgument(0));
    }

    private void noBusinessCalls() {
        verifyNoInteractions(getWalletUseCase, loadWalletPort, saveWalletPort,
                loadLinkedBankPort, saveTransactionPort, loadTransactionPort);
    }
}
