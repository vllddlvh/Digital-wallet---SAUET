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
@Schema(description = "Kết quả xác thực đăng nhập trả về kèm JWT Token")
public class AuthResponse {

	@Schema(description = "Mã JWT Bearer Access Token")
	private String accessToken;

	@Schema(example = "Bearer")
	private String tokenType;

	@Schema(example = "86400", description = "Thời hạn hiệu lực tính theo giây")
	private long expiresIn;

	@Schema(example = "1")
	private Long userId;

	@Schema(example = "customer_01")
	private String username;

	@Schema(example = "1")
	private Long walletId;
}
