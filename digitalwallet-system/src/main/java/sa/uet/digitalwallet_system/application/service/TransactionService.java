package sa.uet.digitalwallet_system.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sa.uet.digitalwallet_system.application.port.in.*;
import sa.uet.digitalwallet_system.application.port.out.*;
import sa.uet.digitalwallet_system.domain.exception.*;
import sa.uet.digitalwallet_system.domain.model.Transaction;
import sa.uet.digitalwallet_system.domain.model.Wallet;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService implements TopupUseCase, WithdrawUseCase, TransferUseCase, GetTransactionsUseCase {

    private final LoadWalletPort loadWalletPort;
    private final SaveWalletPort saveWalletPort;
    private final LoadLinkedBankPort loadLinkedBankPort;
    private final SaveTransactionPort saveTransactionPort;
    private final LoadTransactionPort loadTransactionPort;

    @Override
    @Transactional
    public Transaction topup(TopupCommand command) {
        Wallet wallet = loadWalletPort.loadById(command.getWalletId())
                .orElseThrow(WalletNotFoundException::new);

        if (!"ACTIVE".equals(wallet.getStatus())) {
            throw new WalletNotActiveException(wallet.getWalletNumber());
        }

        // Verify bank account belongs to user's wallet
        loadLinkedBankPort.loadByIdAndWalletId(command.getLinkedBankAccountId(), wallet.getId())
                .orElseThrow(LinkedBankNotFoundException::new);

        // Credit wallet
        Wallet updated = wallet.withBalance(wallet.getBalance() + command.getAmount())
                .withUpdatedAt(LocalDateTime.now());
        saveWalletPort.save(updated);

        // Append ledger entry
        Transaction tx = Transaction.builder()
                .referenceCode(UUID.randomUUID().toString())
                .type("TOPUP")
                .sourceWalletId(null)
                .targetWalletId(wallet.getId())
                .linkedBankAccountId(command.getLinkedBankAccountId())
                .amount(command.getAmount())
                .description(command.getDescription())
                .createdAt(LocalDateTime.now())
                .build();

        return saveTransactionPort.save(tx);
    }

    @Override
    @Transactional
    public Transaction withdraw(WithdrawCommand command) {
        Wallet wallet = loadWalletPort.loadById(command.getWalletId())
                .orElseThrow(WalletNotFoundException::new);

        if (!"ACTIVE".equals(wallet.getStatus())) {
            throw new WalletNotActiveException(wallet.getWalletNumber());
        }
        if (wallet.getBalance() < command.getAmount()) {
            throw new InsufficientBalanceException();
        }

        // Verify bank account belongs to user's wallet
        loadLinkedBankPort.loadByIdAndWalletId(command.getLinkedBankAccountId(), wallet.getId())
                .orElseThrow(LinkedBankNotFoundException::new);

        // Debit wallet
        Wallet updated = wallet.withBalance(wallet.getBalance() - command.getAmount())
                .withUpdatedAt(LocalDateTime.now());
        saveWalletPort.save(updated);

        Transaction tx = Transaction.builder()
                .referenceCode(UUID.randomUUID().toString())
                .type("WITHDRAW")
                .sourceWalletId(wallet.getId())
                .targetWalletId(null)
                .linkedBankAccountId(command.getLinkedBankAccountId())
                .amount(command.getAmount())
                .description(command.getDescription())
                .createdAt(LocalDateTime.now())
                .build();

        return saveTransactionPort.save(tx);
    }

    @Override
    @Transactional
    public Transaction transfer(TransferCommand command) {
        Wallet source = loadWalletPort.loadById(command.getSourceWalletId())
                .orElseThrow(WalletNotFoundException::new);
        Wallet target = loadWalletPort.loadByWalletNumber(command.getTargetWalletNumber())
                .orElseThrow(() -> new WalletNotFoundException("Target wallet not found: " + command.getTargetWalletNumber()));

        if (source.getId().equals(target.getId())) {
            throw new IllegalArgumentException("Cannot transfer to the same wallet");
        }
        if (!"ACTIVE".equals(source.getStatus())) {
            throw new WalletNotActiveException(source.getWalletNumber());
        }
        if (!"ACTIVE".equals(target.getStatus())) {
            throw new WalletNotActiveException(target.getWalletNumber());
        }
        if (source.getBalance() < command.getAmount()) {
            throw new InsufficientBalanceException();
        }

        LocalDateTime now = LocalDateTime.now();

        // Atomic debit source, credit target
        saveWalletPort.save(source.withBalance(source.getBalance() - command.getAmount()).withUpdatedAt(now));
        saveWalletPort.save(target.withBalance(target.getBalance() + command.getAmount()).withUpdatedAt(now));

        Transaction tx = Transaction.builder()
                .referenceCode(UUID.randomUUID().toString())
                .type("TRANSFER")
                .sourceWalletId(source.getId())
                .targetWalletId(target.getId())
                .linkedBankAccountId(null)
                .amount(command.getAmount())
                .description(command.getDescription())
                .createdAt(now)
                .build();

        return saveTransactionPort.save(tx);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactions(Long walletId) {
        return loadTransactionPort.loadByWalletId(walletId);
    }
}
