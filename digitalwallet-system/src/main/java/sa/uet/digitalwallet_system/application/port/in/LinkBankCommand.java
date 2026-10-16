package sa.uet.digitalwallet_system.application.port.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class LinkBankCommand {
    Long walletId;

    @NotBlank @Size(max = 20)
    String bankCode;

    @NotBlank @Size(max = 100)
    String bankName;

    @NotBlank @Size(max = 50)
    String accountNumber;

    @NotBlank @Size(max = 100)
    String holderName;
}
