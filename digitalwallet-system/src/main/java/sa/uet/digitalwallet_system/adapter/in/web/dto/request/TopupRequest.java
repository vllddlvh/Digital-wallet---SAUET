package sa.uet.digitalwallet_system.adapter.in.web.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TopupRequest {
    @NotNull
    private Long linkedBankAccountId;

    @NotNull
    @Min(value = 1, message = "Amount must be greater than 0")
    private Long amount;

    @Size(max = 255)
    private String description;
}
