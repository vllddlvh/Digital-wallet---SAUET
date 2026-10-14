package io.reflectoring.digitalwallet.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Yêu cầu chuyển tiền qua ví điện tử")
public class TransferRequest {

	@NotNull(message = "Target wallet ID không được để trống")
	@Schema(example = "2", description = "Mã số ví thụ hưởng")
	private Long targetWalletId;

	@NotNull(message = "Số tiền không được để trống")
	@Positive(message = "Số tiền chuyển phải là số dương lớn hơn 0")
	@Schema(example = "500000", description = "Số tiền cần chuyển")
	private Long amount;

	@Schema(example = "Thanh toan tien an trua", description = "Nội dung chuyển tiền")
	private String description;
}
