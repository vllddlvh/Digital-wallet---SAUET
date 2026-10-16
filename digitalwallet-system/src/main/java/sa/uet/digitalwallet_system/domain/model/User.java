package sa.uet.digitalwallet_system.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class User {
    Long id;
    String username;
    String passwordHash;
    String fullName;
    String email;
    String role;
    LocalDateTime createdAt;
}
