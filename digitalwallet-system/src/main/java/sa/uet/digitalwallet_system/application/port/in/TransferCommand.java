package sa.uet.digitalwallet_system.application.port.in;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class TransferCommand {
    @NotNull
    Long sourceWalletId;

    @NotBlank
    String targetWalletNumber;

    @NotNull @Min(1)
    Long amount;

    String description;
}
