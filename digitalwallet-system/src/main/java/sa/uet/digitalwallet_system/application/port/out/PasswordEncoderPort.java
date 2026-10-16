package sa.uet.digitalwallet_system.application.port.out;

public interface PasswordEncoderPort {
    String encode(String rawPassword);
}
