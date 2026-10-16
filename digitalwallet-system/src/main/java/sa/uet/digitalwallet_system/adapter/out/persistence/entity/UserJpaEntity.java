package sa.uet.digitalwallet_system.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.sql.Timestamp;

@Entity
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserJpaEntity {

    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
            @Id
    int userId;

    @Column(name = "username")
    String userName;

    @Column(name = "password_hash")
    String passWord;

    @Column(name = "full_name")
    String fullName;

    @Column(name = "email")
    String email;

    @Column(name = "role")
    String role;

    @Column(name = "created_at")
    Timestamp createdAt;
}
