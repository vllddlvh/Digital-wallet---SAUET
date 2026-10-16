package sa.uet.digitalwallet_system.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sa.uet.digitalwallet_system.adapter.out.persistence.entity.UserJpaEntity;

public interface UserRepository extends JpaRepository<UserJpaEntity, Integer> {
}
