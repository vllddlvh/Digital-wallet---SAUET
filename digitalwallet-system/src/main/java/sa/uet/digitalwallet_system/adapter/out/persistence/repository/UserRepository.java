package sa.uet.digitalwallet_system.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sa.uet.digitalwallet_system.adapter.out.persistence.entity.UserJpaEntity;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserJpaEntity, Long> {
    Optional<UserJpaEntity> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
