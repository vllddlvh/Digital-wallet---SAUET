package io.reflectoring.digitalwallet.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Yêu cầu liên kết tài khoản ngân hàng mới")
public class LinkBankAccountRequest {

	@NotBlank(message = "Mã ngân hàng không được để trống")
	@Schema(example = "VCB", description = "Mã viết tắt ngân hàng (VCB, TCB, MB)")
	private String bankCode;

	@NotBlank(message = "Tên ngân hàng không được để trống")
	@Schema(example = "Vietcombank")
	private String bankName;

	@NotBlank(message = "Số tài khoản ngân hàng không được để trống")
	@Schema(example = "0123456789")
	private String accountNumber;

	@NotBlank(message = "Tên chủ tài khoản không được để trống")
	@Schema(example = "NGUYEN VAN A")
	private String accountHolderName;
}
