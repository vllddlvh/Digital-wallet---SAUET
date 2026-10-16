package sa.uet.digitalwallet_system.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sa.uet.digitalwallet_system.adapter.out.persistence.entity.UserJpaEntity;
import sa.uet.digitalwallet_system.adapter.out.persistence.mapper.UserMapper;
import sa.uet.digitalwallet_system.adapter.out.persistence.repository.UserRepository;
import sa.uet.digitalwallet_system.application.port.out.LoadUserPort;
import sa.uet.digitalwallet_system.application.port.out.SaveUserPort;
import sa.uet.digitalwallet_system.domain.model.User;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements LoadUserPort, SaveUserPort {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public Optional<User> loadByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = userMapper.toEntity(user);
        UserJpaEntity saved = userRepository.save(entity);
        return userMapper.toDomain(saved);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
