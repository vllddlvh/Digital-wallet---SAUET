package sa.uet.digitalwallet_system.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sa.uet.digitalwallet_system.application.port.in.LoginCommand;
import sa.uet.digitalwallet_system.application.port.in.LoginUseCase;
import sa.uet.digitalwallet_system.application.port.out.LoadUserPort;
import sa.uet.digitalwallet_system.application.port.out.PasswordVerifierPort;
import sa.uet.digitalwallet_system.application.port.out.TokenGeneratorPort;
import sa.uet.digitalwallet_system.domain.exception.InvalidCredentialsException;
import sa.uet.digitalwallet_system.domain.model.User;

@Service
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {

    private final LoadUserPort loadUserPort;
    private final PasswordVerifierPort passwordVerifierPort;
    private final TokenGeneratorPort tokenGeneratorPort;

    @Override
    @Transactional(readOnly = true)
    public String login(LoginCommand command) {
        User user = loadUserPort.loadByUsername(command.getUsername())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordVerifierPort.matches(command.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return tokenGeneratorPort.generateToken(user);
    }
}
