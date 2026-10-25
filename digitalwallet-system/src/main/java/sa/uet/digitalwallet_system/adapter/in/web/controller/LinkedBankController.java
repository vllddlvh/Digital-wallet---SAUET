package sa.uet.digitalwallet_system.adapter.in.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sa.uet.digitalwallet_system.adapter.in.web.dto.request.LinkBankRequest;
import sa.uet.digitalwallet_system.adapter.in.web.dto.response.ApiResponse;
import sa.uet.digitalwallet_system.adapter.in.web.dto.response.ErrorResponse;
import sa.uet.digitalwallet_system.adapter.in.web.dto.response.LinkedBankResponse;
import sa.uet.digitalwallet_system.application.port.in.GetLinkedBanksUseCase;
import sa.uet.digitalwallet_system.application.port.in.GetWalletUseCase;
import sa.uet.digitalwallet_system.application.port.in.LinkBankCommand;
import sa.uet.digitalwallet_system.application.port.in.LinkBankUseCase;
import sa.uet.digitalwallet_system.application.port.in.UnlinkBankUseCase;
import sa.uet.digitalwallet_system.domain.model.LinkedBankAccount;
import sa.uet.digitalwallet_system.domain.model.Wallet;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/linked-banks")
@RequiredArgsConstructor
@Tag(name = "Linked Banks", description = "Endpoints for linking, listing, and unlinking bank accounts")
@SecurityRequirement(name = "bearerAuth")
public class LinkedBankController {

    private final GetWalletUseCase getWalletUseCase;
    private final LinkBankUseCase linkBankUseCase;
    private final GetLinkedBanksUseCase getLinkedBanksUseCase;
    private final UnlinkBankUseCase unlinkBankUseCase;

    @PostMapping
    @Operation(summary = "Link a new bank account", description = "Links a bank account to the authenticated user's wallet")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Bank account linked successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Wallet not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<LinkedBankResponse>> linkBank(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody LinkBankRequest request) {
        Wallet wallet = getWalletUseCase.getWallet(userId);
        LinkBankCommand command = new LinkBankCommand(
                wallet.getId(),
                request.getBankCode(),
                request.getBankName(),
                request.getAccountNumber(),
                request.getHolderName()
        );
        LinkedBankAccount linkedAccount = linkBankUseCase.link(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Bank account linked successfully", LinkedBankResponse.fromDomain(linkedAccount)));
    }

    @GetMapping
    @Operation(summary = "Get linked bank accounts", description = "Returns active linked bank accounts for current user's wallet")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Linked bank accounts retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Wallet not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<List<LinkedBankResponse>>> getLinkedBanks(
            @AuthenticationPrincipal Long userId) {
        Wallet wallet = getWalletUseCase.getWallet(userId);
        List<LinkedBankResponse> responses = getLinkedBanksUseCase.getLinkedBanks(wallet.getId())
                .stream()
                .map(LinkedBankResponse::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Unlink bank account", description = "Soft-deletes a linked bank account by setting status to UNLINKED")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bank account unlinked"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Linked bank account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<Void>> unlinkBank(
            @AuthenticationPrincipal Long userId,
            @PathVariable("id") Long id) {
        Wallet wallet = getWalletUseCase.getWallet(userId);
        unlinkBankUseCase.unlink(id, wallet.getId());
        return ResponseEntity.ok(ApiResponse.success("Bank account unlinked successfully", null));
    }
}

