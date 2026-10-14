package io.reflectoring.digitalwallet.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAccountRepository extends JpaRepository<AccountJpaEntity, Long> {
	Optional<AccountJpaEntity> findByUserId(Long userId);
}
