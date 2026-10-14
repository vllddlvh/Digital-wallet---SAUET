package io.reflectoring.digitalwallet.application.domain;

import io.reflectoring.digitalwallet.application.domain.model.Money;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

	@Test
	void zeroConstant() {
		assertThat(Money.ZERO.getAmount()).isEqualTo(BigInteger.ZERO);
	}

	@Test
	void ofFactoryMethod() {
		Money money = Money.of(1500L);
		assertThat(money.getAmount()).isEqualTo(BigInteger.valueOf(1500L));
	}

	@Test
	void isPositiveOrZero() {
		assertThat(Money.of(100L).isPositiveOrZero()).isTrue();
		assertThat(Money.of(0L).isPositiveOrZero()).isTrue();
		assertThat(Money.of(-1L).isPositiveOrZero()).isFalse();
	}

	@Test
	void isNegative() {
		assertThat(Money.of(-100L).isNegative()).isTrue();
		assertThat(Money.of(0L).isNegative()).isFalse();
		assertThat(Money.of(100L).isNegative()).isFalse();
	}

	@Test
	void isPositive() {
		assertThat(Money.of(1L).isPositive()).isTrue();
		assertThat(Money.of(0L).isPositive()).isFalse();
		assertThat(Money.of(-1L).isPositive()).isFalse();
	}

	@Test
	void isGreaterThanOrEqualTo() {
		Money m100 = Money.of(100L);
		Money m50 = Money.of(50L);
		Money m100Copy = Money.of(100L);

		assertThat(m100.isGreaterThanOrEqualTo(m50)).isTrue();
		assertThat(m100.isGreaterThanOrEqualTo(m100Copy)).isTrue();
		assertThat(m50.isGreaterThanOrEqualTo(m100)).isFalse();
	}

	@Test
	void isGreaterThan() {
		Money m100 = Money.of(100L);
		Money m50 = Money.of(50L);
		Money m100Copy = Money.of(100L);

		assertThat(m100.isGreaterThan(m50)).isTrue();
		assertThat(m100.isGreaterThan(m100Copy)).isFalse();
		assertThat(m50.isGreaterThan(m100)).isFalse();
	}

	@Test
	void plusAndAdd() {
		Money a = Money.of(1200L);
		Money b = Money.of(800L);

		Money sumInstance = a.plus(b);
		Money sumStatic = Money.add(a, b);

		assertThat(sumInstance).isEqualTo(Money.of(2000L));
		assertThat(sumStatic).isEqualTo(Money.of(2000L));
	}

	@Test
	void minusAndSubtract() {
		Money a = Money.of(2000L);
		Money b = Money.of(800L);

		Money diffInstance = a.minus(b);
		Money diffStatic = Money.subtract(a, b);

		assertThat(diffInstance).isEqualTo(Money.of(1200L));
		assertThat(diffStatic).isEqualTo(Money.of(1200L));
	}

	@Test
	void negate() {
		Money positive = Money.of(500L);
		Money negated = positive.negate();

		assertThat(negated).isEqualTo(Money.of(-500L));
		assertThat(negated.negate()).isEqualTo(positive);
	}

	@Test
	void nullAmountThrowsException() {
		assertThatThrownBy(() -> new Money(null))
				.isInstanceOf(NullPointerException.class);
	}
}
