package io.reflectoring.digitalwallet.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataLinkedBankAccountRepository extends JpaRepository<LinkedBankAccountJpaEntity, Long> {

	List<LinkedBankAccountJpaEntity> findByAccountIdAndStatus(Long accountId, String status);

	List<LinkedBankAccountJpaEntity> findByAccountId(Long accountId);
}
