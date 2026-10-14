package io.reflectoring.digitalwallet.adapter.out.persistence;

import io.reflectoring.digitalwallet.application.domain.model.Account;
import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;
import io.reflectoring.digitalwallet.application.domain.model.Activity;
import io.reflectoring.digitalwallet.application.domain.model.Activity.ActivityId;
import io.reflectoring.digitalwallet.application.domain.model.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccountMapperTest {

	private final AccountMapper mapper = new AccountMapper();

	@Test
	void mapToDomainEntity() {
		AccountJpaEntity accountEntity = new AccountJpaEntity(1L, 10L);
		ActivityJpaEntity activityEntity = new ActivityJpaEntity(
				100L,
				LocalDateTime.of(2023, 1, 1, 12, 0),
				1L,
				1L,
				2L,
				500L
		);

		Account account = mapper.mapToDomainEntity(
				accountEntity,
				List.of(activityEntity),
				200L, // withdrawalBalance
				1000L // depositBalance
		);

		assertThat(account.getId().get().getValue()).isEqualTo(1L);
		assertThat(account.calculateBalance()).isEqualTo(Money.of(300L)); // 1000 - 200 - 500 = 300
		assertThat(account.getActivityWindow().getActivities()).hasSize(1);

		Activity activity = account.getActivityWindow().getActivities().get(0);
		assertThat(activity.getId().getValue()).isEqualTo(100L);
		assertThat(activity.getSourceAccountId().getValue()).isEqualTo(1L);
		assertThat(activity.getTargetAccountId().getValue()).isEqualTo(2L);
		assertThat(activity.getMoney()).isEqualTo(Money.of(500L));
	}

	@Test
	void mapToJpaEntity() {
		Activity activity = new Activity(
				new ActivityId(100L),
				new AccountId(1L),
				new AccountId(1L),
				new AccountId(2L),
				LocalDateTime.of(2023, 1, 1, 12, 0),
				Money.of(500L)
		);

		ActivityJpaEntity entity = mapper.mapToJpaEntity(activity);

		assertThat(entity.getId()).isEqualTo(100L);
		assertThat(entity.getOwnerAccountId()).isEqualTo(1L);
		assertThat(entity.getSourceAccountId()).isEqualTo(1L);
		assertThat(entity.getTargetAccountId()).isEqualTo(2L);
		assertThat(entity.getAmount()).isEqualTo(500L);
		assertThat(entity.getTimestamp()).isEqualTo(LocalDateTime.of(2023, 1, 1, 12, 0));
	}
}
