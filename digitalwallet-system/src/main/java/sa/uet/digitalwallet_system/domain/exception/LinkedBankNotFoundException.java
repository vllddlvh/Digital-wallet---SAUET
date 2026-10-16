package sa.uet.digitalwallet_system.domain.exception;

public class LinkedBankNotFoundException extends RuntimeException {
    public LinkedBankNotFoundException() { super("Linked bank account not found"); }
}
