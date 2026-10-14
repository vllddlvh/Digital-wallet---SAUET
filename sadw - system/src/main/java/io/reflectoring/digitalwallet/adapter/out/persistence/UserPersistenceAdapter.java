package io.reflectoring.digitalwallet.adapter.out.persistence;

import io.reflectoring.digitalwallet.application.domain.model.Account.AccountId;
import io.reflectoring.digitalwallet.application.domain.model.User;
import io.reflectoring.digitalwallet.application.domain.model.User.UserId;
import io.reflectoring.digitalwallet.application.port.out.CreateAccountPort;
import io.reflectoring.digitalwallet.application.port.out.CreateUserPort;
import io.reflectoring.digitalwallet.application.port.out.LoadUserPort;
import io.reflectoring.digitalwallet.common.PersistenceAdapter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
@PersistenceAdapter
class UserPersistenceAdapter implements LoadUserPort, CreateUserPort, CreateAccountPort {

	private final SpringDataUserRepository userRepository;
	private final SpringDataAccountRepository accountRepository;

	@Override
	public Optional<User> loadUserByUsername(String username) {
		return userRepository.findByUsername(username)
				.map(entity -> new User(
						new UserId(entity.getId()),
						entity.getUsername(),
						entity.getPasswordHash(),
						entity.getFullName(),
						entity.getEmail(),
						entity.getRole()
				));
	}

	@Override
	public boolean existsByUsername(String username) {
		return userRepository.existsByUsername(username);
	}

	@Override
	public Long findWalletIdByUserId(Long userId) {
		return accountRepository.findByUserId(userId)
				.map(AccountJpaEntity::getId)
				.orElse(null);
	}

	@Override
	public User createUser(User user) {
		UserJpaEntity entity = new UserJpaEntity(
				null,
				user.getUsername(),
				user.getPasswordHash(),
				user.getFullName(),
				user.getEmail(),
				user.getRole()
		);
		UserJpaEntity saved = userRepository.save(entity);
		return new User(
				new UserId(saved.getId()),
				saved.getUsername(),
				saved.getPasswordHash(),
				saved.getFullName(),
				saved.getEmail(),
				saved.getRole()
		);
	}

	@Override
	public AccountId createAccount(Long userId) {
		AccountJpaEntity account = new AccountJpaEntity(null, userId);
		AccountJpaEntity saved = accountRepository.save(account);
		return new AccountId(saved.getId());
	}
}
