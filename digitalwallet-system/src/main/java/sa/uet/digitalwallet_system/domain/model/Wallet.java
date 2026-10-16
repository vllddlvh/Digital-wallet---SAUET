package sa.uet.digitalwallet_system.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;

@Value
@Builder
@With
public class Wallet {
    Long id;
    Long userId;
    String walletNumber;
    Long balance;
    String status;
    Long version;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
