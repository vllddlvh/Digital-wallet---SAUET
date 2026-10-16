package sa.uet.digitalwallet_system.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LinkBankRequest {
    @NotBlank
    @Size(max = 20)
    private String bankCode;

    @NotBlank
    @Size(max = 100)
    private String bankName;

    @NotBlank
    @Size(max = 50)
    private String accountNumber;

    @NotBlank
    @Size(max = 100)
    private String holderName;
}
