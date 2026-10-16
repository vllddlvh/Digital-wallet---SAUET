package sa.uet.digitalwallet_system.adapter.in.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sa.uet.digitalwallet_system.domain.model.Transaction;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private String referenceCode;
    private String type;
    private Long sourceWalletId;
    private Long targetWalletId;
    private Long linkedBankAccountId;
    private Long amount;
    private String description;
    private LocalDateTime createdAt;

    public static TransactionResponse fromDomain(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .referenceCode(tx.getReferenceCode())
                .type(tx.getType())
                .sourceWalletId(tx.getSourceWalletId())
                .targetWalletId(tx.getTargetWalletId())
                .linkedBankAccountId(tx.getLinkedBankAccountId())
                .amount(tx.getAmount())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
