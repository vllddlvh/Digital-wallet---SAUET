package sa.uet.digitalwallet_system.application.port.in;

import sa.uet.digitalwallet_system.domain.model.Wallet;

public interface GetWalletUseCase {
    Wallet getWallet(Long userId);
}
