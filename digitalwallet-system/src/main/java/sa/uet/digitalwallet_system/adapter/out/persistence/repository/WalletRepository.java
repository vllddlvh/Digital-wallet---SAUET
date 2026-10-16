package sa.uet.digitalwallet_system.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sa.uet.digitalwallet_system.adapter.out.persistence.entity.WalletJpaEntity;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<WalletJpaEntity, Long> {
    Optional<WalletJpaEntity> findByUserId(Long userId);
    Optional<WalletJpaEntity> findByWalletNumber(String walletNumber);
}
