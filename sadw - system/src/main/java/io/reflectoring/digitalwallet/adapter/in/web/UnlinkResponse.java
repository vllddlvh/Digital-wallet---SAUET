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
@Schema(description = "Kết quả hủy liên kết tài khoản ngân hàng")
public class UnlinkResponse {

	@Schema(example = "1")
	private Long unlinkedAccountId;

	@Schema(example = "42")
	private Long walletId;

	@Schema(example = "UNLINKED")
	private String status;

	@Schema(description = "Thời điểm hủy liên kết")
	private LocalDateTime unlinkedAt;
}
