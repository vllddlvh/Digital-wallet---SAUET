package sa.uet.digitalwallet_system.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "linked_bank_accounts")
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LinkedBankAccountJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "wallet_id", nullable = false)
    Long walletId;

    @Column(name = "bank_code", nullable = false, length = 20)
    String bankCode;

    @Column(name = "bank_name", nullable = false, length = 100)
    String bankName;

    @Column(name = "account_number_masked", nullable = false, length = 50)
    String accountNumberMasked;

    @Column(name = "holder_name", nullable = false, length = 100)
    String holderName;

    @Column(name = "status", nullable = false, length = 20)
    String status;

    @Column(name = "linked_at", nullable = false, updatable = false)
    LocalDateTime linkedAt;

    @Column(name = "unlinked_at")
    LocalDateTime unlinkedAt;
}
