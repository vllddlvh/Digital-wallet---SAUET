package io.reflectoring.digitalwallet.adapter.out.persistence;

import io.reflectoring.digitalwallet.application.domain.model.Account;
import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;
import io.reflectoring.digitalwallet.application.domain.model.Activity;
import io.reflectoring.digitalwallet.application.port.out.LoadAccountPort;
import io.reflectoring.digitalwallet.application.port.out.UpdateAccountStatePort;
import io.reflectoring.digitalwallet.common.PersistenceAdapter;
import lombok.RequiredArgsConstructor;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@PersistenceAdapter
class AccountPersistenceAdapter implements
		LoadAccountPort,
		UpdateAccountStatePort {

	private final SpringDataAccountRepository accountRepository;
	private final ActivityRepository activityRepository;
	private final AccountMapper accountMapper;

	@Override
	public Account loadAccount(
					AccountId accountId,
					LocalDateTime baselineDate) {

		AccountJpaEntity account =
				accountRepository.findById(accountId.getValue())
						.orElseThrow(EntityNotFoundException::new);

		List<ActivityJpaEntity> activities =
				activityRepository.findByOwnerSince(
						accountId.getValue(),
						baselineDate);

		Long withdrawalBalance = activityRepository
				.getWithdrawalBalanceUntil(
						accountId.getValue(),
						baselineDate)
				.orElse(0L);

		Long depositBalance = activityRepository
				.getDepositBalanceUntil(
						accountId.getValue(),
						baselineDate)
				.orElse(0L);

		return accountMapper.mapToDomainEntity(
				account,
				activities,
				withdrawalBalance,
				depositBalance);

	}

	@Override
	public void updateActivities(Account account) {
		for (Activity activity : account.getActivityWindow().getActivities()) {
			if (activity.getId() == null) {
				activityRepository.save(accountMapper.mapToJpaEntity(activity));
			}
		}
	}

}
