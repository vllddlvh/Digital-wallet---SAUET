package sa.uet.digitalwallet_system.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sa.uet.digitalwallet_system.adapter.out.persistence.entity.LinkedBankAccountJpaEntity;

import java.util.List;
import java.util.Optional;

public interface LinkedBankAccountRepository extends JpaRepository<LinkedBankAccountJpaEntity, Long> {
    List<LinkedBankAccountJpaEntity> findByWalletIdAndStatus(Long walletId, String status);
    Optional<LinkedBankAccountJpaEntity> findByIdAndWalletId(Long id, Long walletId);
}
