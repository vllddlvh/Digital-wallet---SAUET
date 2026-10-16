package sa.uet.digitalwallet_system.application.port.in;

public interface LoginUseCase {
    /**
     * Authenticates user and returns a JWT access token.
     */
    String login(LoginCommand command);
}
