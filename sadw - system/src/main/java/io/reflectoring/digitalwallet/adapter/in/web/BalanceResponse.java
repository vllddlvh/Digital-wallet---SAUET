package io.reflectoring.digitalwallet.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Thông tin số dư ví điện tử")
public class BalanceResponse {

	@Schema(example = "42", description = "Mã số định danh ví")
	private Long walletId;

	@Schema(example = "VND")
	private String currency;

	@Schema(example = "2500000", description = "Số dư hiện tại")
	private Long currentBalance;

	@Schema(description = "Thời điểm cập nhật số dư")
	private LocalDateTime lastUpdated;
}
