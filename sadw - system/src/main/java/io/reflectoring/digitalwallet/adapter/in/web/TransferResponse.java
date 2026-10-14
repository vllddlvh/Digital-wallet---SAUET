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
@Schema(description = "Kết quả giao dịch chuyển tiền")
public class TransferResponse {

	@Schema(example = "1")
	private Long sourceWalletId;

	@Schema(example = "2")
	private Long targetWalletId;

	@Schema(example = "500000")
	private Long amount;

	@Schema(example = "SUCCESS")
	private String status;

	@Schema(description = "Thời điểm thực hiện giao dịch")
	private LocalDateTime timestamp;
}
