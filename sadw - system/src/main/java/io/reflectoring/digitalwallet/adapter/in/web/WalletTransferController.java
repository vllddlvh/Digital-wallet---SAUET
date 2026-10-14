package io.reflectoring.digitalwallet.adapter.in.web;

import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;
import io.reflectoring.digitalwallet.application.domain.model.Money;
import io.reflectoring.digitalwallet.application.port.in.SendMoneyCommand;
import io.reflectoring.digitalwallet.application.port.in.SendMoneyUseCase;
import io.reflectoring.digitalwallet.common.WebAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@WebAdapter
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Transaction & Transfer", description = "Các endpoint thực hiện giao dịch chuyển tiền và thanh toán")
public class WalletTransferController {

	private final SendMoneyUseCase sendMoneyUseCase;

	@Operation(
			summary = "Chuyển tiền ngang hàng P2P (Protected - POST)",
			description = "Thực hiện lệnh chuyển tiền từ ví nguồn sang ví nhận qua JSON payload. Endpoint yêu cầu xác thực JWT qua Filter.",
			security = @SecurityRequirement(name = "BearerAuth")
	)
	@PostMapping("/{sourceWalletId}/transfer")
	public ResponseEntity<ApiResponse<TransferResponse>> transfer(
			@PathVariable("sourceWalletId") Long sourceWalletId,
			@Valid @RequestBody TransferRequest request) {

		SendMoneyCommand command = new SendMoneyCommand(
				new AccountId(sourceWalletId),
				new AccountId(request.getTargetWalletId()),
				Money.of(request.getAmount())
		);

		boolean success = sendMoneyUseCase.sendMoney(command);

		if (!success) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(ApiResponse.error("TRANSFER_FAILED", "Giao dịch thất bại: Số dư khả dụng không đủ để chuyển tiền"));
		}

		TransferResponse response = TransferResponse.builder()
				.sourceWalletId(sourceWalletId)
				.targetWalletId(request.getTargetWalletId())
				.amount(request.getAmount())
				.status("SUCCESS")
				.timestamp(LocalDateTime.now())
				.build();

		return ResponseEntity.ok(ApiResponse.ok("Chuyển tiền thành công", response));
	}
}
