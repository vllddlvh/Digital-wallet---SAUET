package io.reflectoring.digitalwallet.application.domain;

import io.reflectoring.digitalwallet.application.domain.model.User;
import io.reflectoring.digitalwallet.application.domain.model.User.UserId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

	@Test
	void userCreationAndGetters() {
		UserId userId = new UserId(1L);
		User user = new User(
				userId,
				"alice",
				"hashed_pass",
				"Alice Nguyen",
				"alice@digitalwallet.io",
				"ROLE_USER"
		);

		assertThat(user.getId()).isEqualTo(userId);
		assertThat(user.getId().getValue()).isEqualTo(1L);
		assertThat(user.getUsername()).isEqualTo("alice");
		assertThat(user.getPasswordHash()).isEqualTo("hashed_pass");
		assertThat(user.getFullName()).isEqualTo("Alice Nguyen");
		assertThat(user.getEmail()).isEqualTo("alice@digitalwallet.io");
		assertThat(user.getRole()).isEqualTo("ROLE_USER");
	}

	@Test
	void userIdEquality() {
		UserId id1 = new UserId(10L);
		UserId id2 = new UserId(10L);
		UserId id3 = new UserId(20L);

		assertThat(id1).isEqualTo(id2);
		assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
		assertThat(id1).isNotEqualTo(id3);
	}

	@Test
	void userValueObjectEquality() {
		User user1 = new User(new UserId(1L), "u", "p", "fn", "e@b.io", "ROLE_USER");
		User user2 = new User(new UserId(1L), "u", "p", "fn", "e@b.io", "ROLE_USER");
		User user3 = new User(new UserId(2L), "u", "p", "fn", "e@b.io", "ROLE_USER");

		assertThat(user1).isEqualTo(user2);
		assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
		assertThat(user1).isNotEqualTo(user3);
	}
}
