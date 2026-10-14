package io.reflectoring.digitalwallet.application.port.in;

import io.reflectoring.digitalwallet.application.domain.model.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PositiveMoneyValidatorTest {

	private final PositiveMoneyValidator validator = new PositiveMoneyValidator();

	@Test
	void isValid_positiveMoney_returnsTrue() {
		assertThat(validator.isValid(Money.of(100L), null)).isTrue();
		assertThat(validator.isValid(Money.of(1L), null)).isTrue();
	}

	@Test
	void isValid_zeroOrNegative_returnsFalse() {
		assertThat(validator.isValid(Money.of(0L), null)).isFalse();
		assertThat(validator.isValid(Money.of(-50L), null)).isFalse();
	}
}
