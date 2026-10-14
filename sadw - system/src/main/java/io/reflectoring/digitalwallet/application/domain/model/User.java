package io.reflectoring.digitalwallet.application.domain.model;

import lombok.Value;

@Value
public class User {
	UserId id;
	String username;
	String passwordHash;
	String fullName;
	String email;
	String role;

	@Value
	public static class UserId {
		Long value;
	}
}
