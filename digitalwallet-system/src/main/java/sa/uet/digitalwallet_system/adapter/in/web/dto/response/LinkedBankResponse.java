package sa.uet.digitalwallet_system.adapter.in.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sa.uet.digitalwallet_system.domain.model.LinkedBankAccount;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkedBankResponse {
    private Long id;
    private Long walletId;
    private String bankCode;
    private String bankName;
    private String accountNumberMasked;
    private String holderName;
    private String status;
    private LocalDateTime linkedAt;
    private LocalDateTime unlinkedAt;

    public static LinkedBankResponse fromDomain(LinkedBankAccount domain) {
        return LinkedBankResponse.builder()
                .id(domain.getId())
                .walletId(domain.getWalletId())
                .bankCode(domain.getBankCode())
                .bankName(domain.getBankName())
                .accountNumberMasked(domain.getAccountNumberMasked())
                .holderName(domain.getHolderName())
                .status(domain.getStatus())
                .linkedAt(domain.getLinkedAt())
                .unlinkedAt(domain.getUnlinkedAt())
                .build();
    }
}
