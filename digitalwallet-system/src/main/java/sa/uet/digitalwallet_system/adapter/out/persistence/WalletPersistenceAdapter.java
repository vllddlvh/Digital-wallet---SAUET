package sa.uet.digitalwallet_system.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sa.uet.digitalwallet_system.adapter.out.persistence.entity.WalletJpaEntity;
import sa.uet.digitalwallet_system.adapter.out.persistence.mapper.WalletMapper;
import sa.uet.digitalwallet_system.adapter.out.persistence.repository.WalletRepository;
import sa.uet.digitalwallet_system.application.port.out.LoadWalletPort;
import sa.uet.digitalwallet_system.application.port.out.SaveWalletPort;
import sa.uet.digitalwallet_system.domain.model.Wallet;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WalletPersistenceAdapter implements LoadWalletPort, SaveWalletPort {

    private final WalletRepository walletRepository;
    private final WalletMapper walletMapper;

    @Override
    public Optional<Wallet> loadByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .map(walletMapper::toDomain);
    }

    @Override
    public Optional<Wallet> loadByWalletNumber(String walletNumber) {
        return walletRepository.findByWalletNumber(walletNumber)
                .map(walletMapper::toDomain);
    }

    @Override
    public Optional<Wallet> loadById(Long id) {
        return walletRepository.findById(id)
                .map(walletMapper::toDomain);
    }

    @Override
    public Wallet save(Wallet wallet) {
        WalletJpaEntity entity = walletMapper.toEntity(wallet);
        WalletJpaEntity saved = walletRepository.save(entity);
        return walletMapper.toDomain(saved);
    }
}
