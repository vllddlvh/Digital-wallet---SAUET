package sa.uet.digitalwallet_system.adapter.in.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SecurityException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String userId = claims.getSubject();
                String role = claims.get("role", String.class);

                List<GrantedAuthority> authorities = new ArrayList<>();
                if (role != null && !role.isBlank()) {
                    String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    String roleWithoutPrefix = role.startsWith("ROLE_") ? role.substring(5) : role;
                    authorities.add(new SimpleGrantedAuthority(roleWithPrefix));
                    if (!roleWithPrefix.equals(roleWithoutPrefix)) {
                        authorities.add(new SimpleGrantedAuthority(roleWithoutPrefix));
                    }
                }

                if (userId != null) {
                    Long principal = Long.parseLong(userId);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            authorities
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (ExpiredJwtException ex) {
                log.debug("JWT token is expired: {}", ex.getMessage());
            } catch (UnsupportedJwtException ex) {
                log.debug("JWT token is unsupported: {}", ex.getMessage());
            } catch (MalformedJwtException ex) {
                log.debug("JWT token is malformed: {}", ex.getMessage());
            } catch (SecurityException ex) {
                log.debug("JWT signature validation failed: {}", ex.getMessage());
            } catch (IllegalArgumentException ex) {
                log.debug("JWT claims string is empty or invalid: {}", ex.getMessage());
            } catch (Exception ex) {
                log.warn("Cannot set user authentication from JWT: {}", ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
