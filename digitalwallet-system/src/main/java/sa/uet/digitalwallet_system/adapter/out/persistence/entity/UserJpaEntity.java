package sa.uet.digitalwallet_system.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    String email;

    @Column(name = "role", nullable = false, length = 20)
    String role;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}
