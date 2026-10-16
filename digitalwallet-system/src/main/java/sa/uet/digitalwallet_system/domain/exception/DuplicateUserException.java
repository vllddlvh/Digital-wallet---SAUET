package sa.uet.digitalwallet_system.domain.exception;

public class DuplicateUserException extends RuntimeException {
    public DuplicateUserException(String field) {
        super("Already exists: " + field);
    }
}
