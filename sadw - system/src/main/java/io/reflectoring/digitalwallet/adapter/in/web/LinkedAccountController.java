package io.reflectoring.digitalwallet.adapter.in.web;

import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;
import io.reflectoring.digitalwallet.application.domain.model.LinkedBankAccount;
import io.reflectoring.digitalwallet.application.port.in.ManageLinkedAccountUseCase;
import io.reflectoring.digitalwallet.application.port.in.ManageLinkedAccountUseCase.LinkBankAccountCommand;
import io.reflectoring.digitalwallet.application.port.in.UnlinkBankAccountUseCase;
import io.reflectoring.digitalwallet.application.port.in.UnlinkBankAccountUseCase.UnlinkBankAccountCommand;
import io.reflectoring.digitalwallet.common.WebAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@WebAdapter
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Linked Accounts", description = "Các endpoint liên kết và hủy liên kết tài khoản ngân hàng")
public class LinkedAccountController {

	private final ManageLinkedAccountUseCase manageLinkedAccountUseCase;
	private final UnlinkBankAccountUseCase unlinkBankAccountUseCase;

	@Operation(
			summary = "Xem danh sách tài khoản ngân hàng liên kết (Protected - GET)",
			security = @SecurityRequirement(name = "BearerAuth")
	)
	@GetMapping("/{walletId}/linked-accounts")
	public ResponseEntity<ApiResponse<List<LinkedAccountResponse>>> getLinkedAccounts(@PathVariable("walletId") Long walletId) {
		List<LinkedBankAccount> accounts = manageLinkedAccountUseCase.getLinkedAccounts(new AccountId(walletId));
		List<LinkedAccountResponse> responses = accounts.stream()
				.map(this::mapToResponse)
				.collect(Collectors.toList());

		return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách thành công", responses));
	}

	@Operation(
			summary = "Thêm liên kết tài khoản ngân hàng mới (Protected - POST)",
			security = @SecurityRequirement(name = "BearerAuth")
	)
	@PostMapping("/{walletId}/linked-accounts")
	public ResponseEntity<ApiResponse<LinkedAccountResponse>> linkAccount(
			@PathVariable("walletId") Long walletId,
			@Valid @RequestBody LinkBankAccountRequest request) {

		LinkBankAccountCommand command = new LinkBankAccountCommand(
				new AccountId(walletId),
				request.getBankCode(),
				request.getBankName(),
				request.getAccountNumber(),
				request.getAccountHolderName()
		);

		LinkedBankAccount saved = manageLinkedAccountUseCase.linkBankAccount(command);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.ok("Liên kết tài khoản ngân hàng thành công", mapToResponse(saved)));
	}

	@Operation(
			summary = "Hủy liên kết tài khoản ngân hàng (Protected - DELETE)",
			description = "Hủy bỏ liên kết thẻ hoặc tài khoản ngân hàng khỏi ví. Yêu cầu xác thực JWT qua Filter.",
			security = @SecurityRequirement(name = "BearerAuth")
	)
	@DeleteMapping("/{walletId}/linked-accounts/{accountId}")
	public ResponseEntity<ApiResponse<UnlinkResponse>> unlinkAccount(
			@PathVariable("walletId") Long walletId,
			@PathVariable("accountId") Long accountId) {

		UnlinkBankAccountCommand command = new UnlinkBankAccountCommand(
				new AccountId(walletId),
				accountId
		);

		boolean success = unlinkBankAccountUseCase.unlinkBankAccount(command);

		if (!success) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ApiResponse.error("NOT_FOUND", "Không thể hủy liên kết tài khoản ngân hàng"));
		}

		UnlinkResponse response = UnlinkResponse.builder()
				.unlinkedAccountId(accountId)
				.walletId(walletId)
				.status("UNLINKED")
				.unlinkedAt(LocalDateTime.now())
				.build();

		return ResponseEntity.ok(ApiResponse.ok("Đã hủy liên kết tài khoản ngân hàng thành công", response));
	}

	private LinkedAccountResponse mapToResponse(LinkedBankAccount entity) {
		String maskedNumber = maskAccountNumber(entity.getAccountNumber());
		return LinkedAccountResponse.builder()
				.id(entity.getId() != null ? entity.getId().getValue() : null)
				.walletId(entity.getWalletId().getValue())
				.bankCode(entity.getBankCode())
				.bankName(entity.getBankName())
				.accountNumber(maskedNumber)
				.accountHolderName(entity.getAccountHolderName())
				.status(entity.getStatus())
				.build();
	}

	private String maskAccountNumber(String accountNumber) {
		if (accountNumber == null || accountNumber.length() <= 4) {
			return "****";
		}
		return "******" + accountNumber.substring(accountNumber.length() - 4);
	}
}
