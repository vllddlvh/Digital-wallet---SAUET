package io.reflectoring.digitalwallet.adapter.in.web.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

	private JwtTokenProvider tokenProvider;
	private JwtAuthenticationFilter filter;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.clearContext();
		tokenProvider = new JwtTokenProvider();
		filter = new JwtAuthenticationFilter(tokenProvider);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void doFilter_withValidBearerToken_authenticatesUser() throws ServletException, IOException {
		String token = tokenProvider.generateToken("alice", 10L, 42L);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + token);
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		assertThat(auth).isNotNull();
		assertThat(auth.isAuthenticated()).isTrue();
		assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedUser.class);

		AuthenticatedUser user = (AuthenticatedUser) auth.getPrincipal();
		assertThat(user.getUsername()).isEqualTo("alice");
		assertThat(user.getUserId()).isEqualTo(10L);
		assertThat(user.getWalletId()).isEqualTo(42L);
	}

	@Test
	void doFilter_withoutAuthorizationHeader_doesNotAuthenticate() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		assertThat(auth).isNull();
	}

	@Test
	void doFilter_withInvalidToken_doesNotAuthenticate() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer invalid-token-string");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		assertThat(auth).isNull();
	}

	@Test
	void doFilter_withNonBearerHeader_doesNotAuthenticate() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Basic user:pass");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		assertThat(auth).isNull();
	}
}
