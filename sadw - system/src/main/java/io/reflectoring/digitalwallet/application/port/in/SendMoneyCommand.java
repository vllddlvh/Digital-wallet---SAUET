package io.reflectoring.digitalwallet.application.port.in;

import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;
import io.reflectoring.digitalwallet.application.domain.model.Money;
import jakarta.validation.constraints.NotNull;

import static io.reflectoring.digitalwallet.common.validation.Validation.validate;

public record SendMoneyCommand(
        @NotNull AccountId sourceAccountId,
        @NotNull AccountId targetAccountId,
        @NotNull @PositiveMoney Money money
) {

    public SendMoneyCommand(
            AccountId sourceAccountId,
            AccountId targetAccountId,
            Money money) {
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.money = money;
        validate(this);
    }

}
