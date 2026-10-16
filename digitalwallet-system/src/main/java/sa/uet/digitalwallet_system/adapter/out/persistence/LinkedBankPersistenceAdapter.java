package sa.uet.digitalwallet_system.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sa.uet.digitalwallet_system.adapter.out.persistence.entity.LinkedBankAccountJpaEntity;
import sa.uet.digitalwallet_system.adapter.out.persistence.mapper.LinkedBankAccountMapper;
import sa.uet.digitalwallet_system.adapter.out.persistence.repository.LinkedBankAccountRepository;
import sa.uet.digitalwallet_system.application.port.out.LoadLinkedBankPort;
import sa.uet.digitalwallet_system.application.port.out.SaveLinkedBankPort;
import sa.uet.digitalwallet_system.domain.model.LinkedBankAccount;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LinkedBankPersistenceAdapter implements LoadLinkedBankPort, SaveLinkedBankPort {

    private final LinkedBankAccountRepository linkedBankAccountRepository;
    private final LinkedBankAccountMapper linkedBankAccountMapper;

    @Override
    public List<LinkedBankAccount> loadActiveByWalletId(Long walletId) {
        return linkedBankAccountRepository.findByWalletIdAndStatus(walletId, "ACTIVE")
                .stream()
                .map(linkedBankAccountMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<LinkedBankAccount> loadByIdAndWalletId(Long id, Long walletId) {
        return linkedBankAccountRepository.findByIdAndWalletId(id, walletId)
                .map(linkedBankAccountMapper::toDomain);
    }

    @Override
    public Optional<LinkedBankAccount> loadById(Long id) {
        return linkedBankAccountRepository.findById(id)
                .map(linkedBankAccountMapper::toDomain);
    }

    @Override
    public LinkedBankAccount save(LinkedBankAccount linkedBankAccount) {
        LinkedBankAccountJpaEntity entity = linkedBankAccountMapper.toEntity(linkedBankAccount);
        LinkedBankAccountJpaEntity saved = linkedBankAccountRepository.save(entity);
        return linkedBankAccountMapper.toDomain(saved);
    }
}
