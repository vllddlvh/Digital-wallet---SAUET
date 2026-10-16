package sa.uet.digitalwallet_system.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class LinkedBankAccount {
    Long id;
    Long walletId;
    String bankCode;
    String bankName;
    String accountNumberMasked;
    String holderName;
    String status;
    LocalDateTime linkedAt;
    LocalDateTime unlinkedAt;
}
