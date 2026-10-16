package sa.uet.digitalwallet_system.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "reference_code", nullable = false, unique = true, length = 36)
    String referenceCode;

    @Column(name = "type", nullable = false, length = 20)
    String type;

    @Column(name = "source_wallet_id")
    Long sourceWalletId;

    @Column(name = "target_wallet_id")
    Long targetWalletId;

    @Column(name = "linked_bank_account_id")
    Long linkedBankAccountId;

    @Column(name = "amount", nullable = false)
    Long amount;

    @Column(name = "description", length = 255)
    String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}
