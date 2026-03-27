package com.bidcast.wallet_service.wallet;

import com.bidcast.wallet_service.core.exception.GlobalExceptionHandler;
import com.bidcast.wallet_service.core.exception.InsufficientWalletBalanceException;
import com.bidcast.wallet_service.core.exception.WalletNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
@Import(GlobalExceptionHandler.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

    @Test
    void getMyWallet_returnsWalletFromHeaderIdentity() throws Exception {
        UUID ownerId = UUID.randomUUID();
        Wallet wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .ownerType(WalletOwnerType.ADVERTISER)
                .currencyCode("ARS")
                .balance(new BigDecimal("500.00"))
                .frozenBalance(new BigDecimal("25.00"))
                .build();

        when(walletService.getWalletByOwner(ownerId, WalletOwnerType.ADVERTISER)).thenReturn(wallet);

        mockMvc.perform(get("/api/v1/wallets/me")
                .header("X-User-Id", ownerId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ownerId").value(ownerId.toString()))
            .andExpect(jsonPath("$.currencyCode").value("ARS"));
    }

    @Test
    void freeze_returnsOk() throws Exception {
        UUID advertiserId = UUID.randomUUID();
        String body = """
                {
                  "advertiserId": "%s",
                  "amount": 50.00,
                  "referenceId": "bid-123",
                  "reason": "auction hold"
                }
                """.formatted(advertiserId);

        mockMvc.perform(post("/api/v1/wallets/freeze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk());

        verify(walletService).freeze(eq(advertiserId), eq(WalletOwnerType.ADVERTISER), eq(new BigDecimal("50.00")), eq("bid-123"));
    }

    @Test
    void getMyWallet_returnsNotFoundWhenWalletDoesNotExist() throws Exception {
        UUID ownerId = UUID.randomUUID();
        when(walletService.getWalletByOwner(ownerId, WalletOwnerType.ADVERTISER))
                .thenThrow(new WalletNotFoundException(ownerId));

        mockMvc.perform(get("/api/v1/wallets/me")
                        .header("X-User-Id", ownerId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void freeze_returnsConflictWhenBalanceIsInsufficient() throws Exception {
        UUID advertiserId = UUID.randomUUID();
        String body = """
                {
                  "advertiserId": "%s",
                  "amount": 500.00,
                  "referenceId": "bid-999",
                  "reason": "auction hold"
                }
                """.formatted(advertiserId);

        doThrow(new InsufficientWalletBalanceException(UUID.randomUUID())).when(walletService)
                .freeze(eq(advertiserId), eq(WalletOwnerType.ADVERTISER), eq(new BigDecimal("500.00")), eq("bid-999"));

        mockMvc.perform(post("/api/v1/wallets/freeze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void unfreeze_invalidBody_returnsBadRequest() throws Exception {
        String body = """
                {
                  "advertiserId": "",
                  "amount": -1,
                  "referenceId": "",
                  "reason": "rollback"
                }
                """;

        mockMvc.perform(post("/api/v1/wallets/unfreeze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.advertiserId").exists())
            .andExpect(jsonPath("$.amount").exists())
            .andExpect(jsonPath("$.referenceId").exists());
    }

    @Test
    void freeze_invalidAdvertiserUuid_returnsBadRequest() throws Exception {
        String body = """
                {
                  "advertiserId": "not-a-uuid",
                  "amount": 50.00,
                  "referenceId": "bid-123",
                  "reason": "auction hold"
                }
                """;

        mockMvc.perform(post("/api/v1/wallets/freeze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid format"));
    }

    @Test
    void getMyWallet_missingHeader_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/me"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.['X-User-Id']").value("Header is required"));
    }
}
