package sa.uet.digitalwallet_system.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class Transaction {
    Long id;
    String referenceCode;
    String type;
    Long sourceWalletId;
    Long targetWalletId;
    Long linkedBankAccountId;
    Long amount;
    String description;
    LocalDateTime createdAt;
}
