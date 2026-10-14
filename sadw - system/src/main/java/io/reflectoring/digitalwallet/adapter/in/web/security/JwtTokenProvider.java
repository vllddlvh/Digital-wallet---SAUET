package io.reflectoring.digitalwallet.adapter.in.web.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

	// Khóa bí mật 256-bit dùng cho HMAC-SHA256
	private final Key key = Keys.hmacShaKeyFor("digitalwallet-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256!".getBytes());

	// Thời hạn token mặc định: 24 giờ (86400 giây)
	private final long validityInMilliseconds = 24 * 60 * 60 * 1000L;

	public String generateToken(String username, Long userId, Long walletId) {
		Date now = new Date();
		Date validity = new Date(now.getTime() + validityInMilliseconds);

		return Jwts.builder()
				.setSubject(username)
				.claim("userId", userId)
				.claim("walletId", walletId)
				.setIssuedAt(now)
				.setExpiration(validity)
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}

	public String getUsername(String token) {
		return getClaims(token).getSubject();
	}

	public Long getUserId(String token) {
		Number userId = getClaims(token).get("userId", Number.class);
		return userId != null ? userId.longValue() : null;
	}

	public Long getWalletId(String token) {
		Number walletId = getClaims(token).get("walletId", Number.class);
		return walletId != null ? walletId.longValue() : null;
	}

	private Claims getClaims(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token)
				.getBody();
	}
}
