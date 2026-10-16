package sa.uet.digitalwallet_system.domain.exception;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException() { super("Insufficient balance"); }
}
