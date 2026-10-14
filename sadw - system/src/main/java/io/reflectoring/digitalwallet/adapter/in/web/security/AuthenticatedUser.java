package io.reflectoring.digitalwallet.adapter.in.web.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.security.Principal;

@Getter
@RequiredArgsConstructor
public class AuthenticatedUser implements Principal {

	private final Long userId;
	private final String username;
	private final Long walletId;

	@Override
	public String getName() {
		return username;
	}
}
