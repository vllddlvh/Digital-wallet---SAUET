package sa.uet.digitalwallet_system.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sa.uet.digitalwallet_system.application.port.in.*;
import sa.uet.digitalwallet_system.application.port.out.*;
import sa.uet.digitalwallet_system.domain.exception.LinkedBankNotFoundException;
import sa.uet.digitalwallet_system.domain.model.LinkedBankAccount;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LinkedBankService implements LinkBankUseCase, GetLinkedBanksUseCase, UnlinkBankUseCase {

    private final SaveLinkedBankPort saveLinkedBankPort;
    private final LoadLinkedBankPort loadLinkedBankPort;

    @Override
    @Transactional
    public LinkedBankAccount link(LinkBankCommand command) {
        LocalDateTime now = LocalDateTime.now();
        LinkedBankAccount account = LinkedBankAccount.builder()
                .walletId(command.getWalletId())
                .bankCode(command.getBankCode())
                .bankName(command.getBankName())
                .accountNumberMasked(maskAccountNumber(command.getAccountNumber()))
                .holderName(command.getHolderName())
                .status("ACTIVE")
                .linkedAt(now)
                .unlinkedAt(null)
                .build();

        return saveLinkedBankPort.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LinkedBankAccount> getLinkedBanks(Long walletId) {
        return loadLinkedBankPort.loadActiveByWalletId(walletId);
    }

    @Override
    @Transactional
    public void unlink(Long linkedBankId, Long walletId) {
        LinkedBankAccount existing = loadLinkedBankPort.loadByIdAndWalletId(linkedBankId, walletId)
                .orElseThrow(LinkedBankNotFoundException::new);

        if (!"ACTIVE".equals(existing.getStatus())) {
            throw new LinkedBankNotFoundException();
        }

        LinkedBankAccount unlinked = LinkedBankAccount.builder()
                .id(existing.getId())
                .walletId(existing.getWalletId())
                .bankCode(existing.getBankCode())
                .bankName(existing.getBankName())
                .accountNumberMasked(existing.getAccountNumberMasked())
                .holderName(existing.getHolderName())
                .status("UNLINKED")
                .linkedAt(existing.getLinkedAt())
                .unlinkedAt(LocalDateTime.now())
                .build();

        saveLinkedBankPort.save(unlinked);
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber;
        }
        int visibleChars = 4;
        String suffix = accountNumber.substring(accountNumber.length() - visibleChars);
        return "******" + suffix;
    }
}
