package sa.uet.digitalwallet_system.application.port.in;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class LoginCommand {
    @NotBlank
    String username;
    @NotBlank
    String password;
}
