package io.reflectoring.digitalwallet.adapter.out.persistence;

import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;
import io.reflectoring.digitalwallet.application.domain.model.LinkedBankAccount;
import io.reflectoring.digitalwallet.application.domain.model.LinkedBankAccount.LinkedAccountId;
import io.reflectoring.digitalwallet.application.port.out.LoadLinkedAccountPort;
import io.reflectoring.digitalwallet.application.port.out.UpdateLinkedAccountPort;
import io.reflectoring.digitalwallet.common.PersistenceAdapter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@PersistenceAdapter
class LinkedAccountPersistenceAdapter implements LoadLinkedAccountPort, UpdateLinkedAccountPort {

	private final SpringDataLinkedBankAccountRepository repository;

	@Override
	public List<LinkedBankAccount> loadLinkedAccounts(AccountId walletId) {
		return repository.findByAccountId(walletId.getValue()).stream()
				.map(this::mapToDomain)
				.collect(Collectors.toList());
	}

	@Override
	public Optional<LinkedBankAccount> loadLinkedAccount(Long linkedAccountId) {
		return repository.findById(linkedAccountId)
				.map(this::mapToDomain);
	}

	@Override
	public LinkedBankAccount saveLinkedAccount(LinkedBankAccount linkedBankAccount) {
		LinkedBankAccountJpaEntity entity = new LinkedBankAccountJpaEntity(
				linkedBankAccount.getId() != null ? linkedBankAccount.getId().getValue() : null,
				linkedBankAccount.getWalletId().getValue(),
				linkedBankAccount.getBankCode(),
				linkedBankAccount.getBankName(),
				linkedBankAccount.getAccountNumber(),
				linkedBankAccount.getAccountHolderName(),
				linkedBankAccount.getStatus(),
				LocalDateTime.now(),
				null
		);
		LinkedBankAccountJpaEntity saved = repository.save(entity);
		return mapToDomain(saved);
	}

	@Override
	public boolean unlinkAccount(Long linkedAccountId) {
		Optional<LinkedBankAccountJpaEntity> entityOpt = repository.findById(linkedAccountId);
		if (entityOpt.isPresent()) {
			LinkedBankAccountJpaEntity entity = entityOpt.get();
			entity.setStatus("UNLINKED");
			entity.setUnlinkedAt(LocalDateTime.now());
			repository.save(entity);
			return true;
		}
		return false;
	}

	private LinkedBankAccount mapToDomain(LinkedBankAccountJpaEntity entity) {
		return new LinkedBankAccount(
				new LinkedAccountId(entity.getId()),
				new AccountId(entity.getAccountId()),
				entity.getBankCode(),
				entity.getBankName(),
				entity.getAccountNumber(),
				entity.getAccountHolderName(),
				entity.getStatus()
		);
	}
}
