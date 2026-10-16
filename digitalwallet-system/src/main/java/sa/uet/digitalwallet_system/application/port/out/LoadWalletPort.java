package sa.uet.digitalwallet_system.application.port.out;

import sa.uet.digitalwallet_system.domain.model.Wallet;

import java.util.Optional;

public interface LoadWalletPort {
    Optional<Wallet> loadByUserId(Long userId);
    Optional<Wallet> loadByWalletNumber(String walletNumber);
    Optional<Wallet> loadById(Long id);
}
