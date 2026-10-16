package sa.uet.digitalwallet_system.application.port.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class RegisterCommand {
    @NotBlank @Size(max = 50)
    String username;

    @NotBlank @Size(min = 6, max = 100)
    String password;

    @NotBlank @Size(max = 100)
    String fullName;

    @NotBlank @Email @Size(max = 100)
    String email;
}
