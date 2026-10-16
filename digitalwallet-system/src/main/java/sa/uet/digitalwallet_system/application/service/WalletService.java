package sa.uet.digitalwallet_system.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sa.uet.digitalwallet_system.application.port.in.GetWalletUseCase;
import sa.uet.digitalwallet_system.application.port.out.LoadWalletPort;
import sa.uet.digitalwallet_system.domain.exception.WalletNotFoundException;
import sa.uet.digitalwallet_system.domain.model.Wallet;

@Service
@RequiredArgsConstructor
public class WalletService implements GetWalletUseCase {

    private final LoadWalletPort loadWalletPort;

    @Override
    @Transactional(readOnly = true)
    public Wallet getWallet(Long userId) {
        return loadWalletPort.loadByUserId(userId)
                .orElseThrow(WalletNotFoundException::new);
    }
}
