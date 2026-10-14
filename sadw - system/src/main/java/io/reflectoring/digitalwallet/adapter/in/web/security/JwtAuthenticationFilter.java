package io.reflectoring.digitalwallet.adapter.in.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider tokenProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request,
									HttpServletResponse response,
									FilterChain filterChain) throws ServletException, IOException {
		String token = resolveToken(request);

		if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
			String username = tokenProvider.getUsername(token);
			Long userId = tokenProvider.getUserId(token);
			Long walletId = tokenProvider.getWalletId(token);

			AuthenticatedUser principal = new AuthenticatedUser(userId, username, walletId);
			UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(
							principal,
							null,
							Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
					);

			SecurityContextHolder.getContext().setAuthentication(authentication);
		}

		filterChain.doFilter(request, response);
	}

	private String resolveToken(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}
}
