package sa.uet.digitalwallet_system.domain.exception;

public class WalletNotActiveException extends RuntimeException {
    public WalletNotActiveException(String walletNumber) {
        super("Wallet is not active: " + walletNumber);
    }
}
