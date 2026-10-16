package sa.uet.digitalwallet_system.application.port.out;

public interface PasswordVerifierPort {
    boolean matches(String rawPassword, String encodedPassword);
}
