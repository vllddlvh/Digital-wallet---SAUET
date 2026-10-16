package sa.uet.digitalwallet_system.application.port.in;

public interface UnlinkBankUseCase {
    void unlink(Long linkedBankId, Long walletId);
}
