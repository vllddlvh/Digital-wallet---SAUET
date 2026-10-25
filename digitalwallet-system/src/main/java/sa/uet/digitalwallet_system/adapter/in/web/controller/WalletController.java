package sa.uet.digitalwallet_system.adapter.in.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sa.uet.digitalwallet_system.adapter.in.web.dto.response.ApiResponse;
import sa.uet.digitalwallet_system.adapter.in.web.dto.response.ErrorResponse;
import sa.uet.digitalwallet_system.adapter.in.web.dto.response.WalletResponse;
import sa.uet.digitalwallet_system.application.port.in.GetWalletUseCase;
import sa.uet.digitalwallet_system.domain.model.Wallet;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Endpoints for managing digital wallet")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final GetWalletUseCase getWalletUseCase;

    @GetMapping("/me")
    @Operation(summary = "Get current user's wallet", description = "Retrieves wallet details and current balance for the authenticated user")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Wallet retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated – invalid or missing JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Wallet not found for current user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(@AuthenticationPrincipal Long userId) {
        Wallet wallet = getWalletUseCase.getWallet(userId);
        return ResponseEntity.ok(ApiResponse.success(WalletResponse.fromDomain(wallet)));
    }
}

