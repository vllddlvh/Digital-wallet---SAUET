package sa.uet.digitalwallet_system.adapter.out.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import sa.uet.digitalwallet_system.application.port.out.PasswordEncoderPort;
import sa.uet.digitalwallet_system.application.port.out.PasswordVerifierPort;

@Component
@RequiredArgsConstructor
public class BcryptPasswordVerifier implements PasswordVerifierPort, PasswordEncoderPort {

    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Override
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
