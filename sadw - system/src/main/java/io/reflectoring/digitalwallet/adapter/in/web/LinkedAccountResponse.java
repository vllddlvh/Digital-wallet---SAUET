package io.reflectoring.digitalwallet.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Thông tin tài khoản ngân hàng liên kết")
public class LinkedAccountResponse {

	@Schema(example = "1")
	private Long id;

	@Schema(example = "42")
	private Long walletId;

	@Schema(example = "VCB")
	private String bankCode;

	@Schema(example = "Vietcombank")
	private String bankName;

	@Schema(example = "******6789")
	private String accountNumber;

	@Schema(example = "NGUYEN VAN A")
	private String accountHolderName;

	@Schema(example = "ACTIVE")
	private String status;
}
