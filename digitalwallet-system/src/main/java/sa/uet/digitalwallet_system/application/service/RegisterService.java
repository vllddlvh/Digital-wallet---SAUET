package sa.uet.digitalwallet_system.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sa.uet.digitalwallet_system.application.port.in.RegisterCommand;
import sa.uet.digitalwallet_system.application.port.in.RegisterUseCase;
import sa.uet.digitalwallet_system.application.port.out.*;
import sa.uet.digitalwallet_system.domain.exception.DuplicateUserException;
import sa.uet.digitalwallet_system.domain.model.User;
import sa.uet.digitalwallet_system.domain.model.Wallet;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class RegisterService implements RegisterUseCase {

    private final SaveUserPort saveUserPort;
    private final SaveWalletPort saveWalletPort;
    private final PasswordEncoderPort passwordEncoderPort;

    @Override
    @Transactional
    public User register(RegisterCommand command) {
        if (saveUserPort.existsByUsername(command.getUsername())) {
            throw new DuplicateUserException("username");
        }
        if (saveUserPort.existsByEmail(command.getEmail())) {
            throw new DuplicateUserException("email");
        }

        LocalDateTime now = LocalDateTime.now();

        User user = User.builder()
                .username(command.getUsername())
                .passwordHash(passwordEncoderPort.encode(command.getPassword()))
                .fullName(command.getFullName())
                .email(command.getEmail())
                .role("ROLE_USER")
                .createdAt(now)
                .build();

        User savedUser = saveUserPort.save(user);

        // Automatically create a wallet for the new user
        Wallet wallet = Wallet.builder()
                .userId(savedUser.getId())
                .walletNumber(generateWalletNumber())
                .balance(0L)
                .status("ACTIVE")
                .version(0L)
                .createdAt(now)
                .updatedAt(now)
                .build();

        saveWalletPort.save(wallet);

        return savedUser;
    }

    private String generateWalletNumber() {
        // 12-digit random wallet number
        long number = (long) (Math.random() * 900_000_000_000L) + 100_000_000_000L;
        return String.valueOf(number);
    }
}
