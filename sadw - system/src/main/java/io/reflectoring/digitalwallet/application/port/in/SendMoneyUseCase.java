package io.reflectoring.digitalwallet.application.port.in;

public interface SendMoneyUseCase {

	boolean sendMoney(SendMoneyCommand command);

}
