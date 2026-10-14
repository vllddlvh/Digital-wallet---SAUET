package io.reflectoring.digitalwallet.adapter.in.web;

import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;
import io.reflectoring.digitalwallet.application.domain.model.Money;
import io.reflectoring.digitalwallet.application.port.in.GetAccountBalanceUseCase;
import io.reflectoring.digitalwallet.application.port.in.GetAccountBalanceUseCase.GetAccountBalanceQuery;
import io.reflectoring.digitalwallet.common.WebAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@WebAdapter
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet Management", description = "Các endpoint quản lý ví và truy vấn số dư")
public class WalletBalanceController {

	private final GetAccountBalanceUseCase getAccountBalanceUseCase;

	@Operation(
			summary = "Truy vấn số dư ví (Protected - GET)",
			description = "Lấy số dư hiện tại của tài khoản ví. Endpoint yêu cầu xác thực JWT qua Filter.",
			security = @SecurityRequirement(name = "BearerAuth")
	)
	@GetMapping("/{walletId}/balance")
	public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(@PathVariable("walletId") Long walletId) {
		GetAccountBalanceQuery query = new GetAccountBalanceQuery(new AccountId(walletId));
		Money balance = getAccountBalanceUseCase.getAccountBalance(query);

		BalanceResponse response = BalanceResponse.builder()
				.walletId(walletId)
				.currency("VND")
				.currentBalance(balance.getAmount().longValue())
				.lastUpdated(LocalDateTime.now())
				.build();

		return ResponseEntity.ok(ApiResponse.ok("Lấy số dư thành công", response));
	}
}
