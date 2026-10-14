package io.reflectoring.digitalwallet.adapter.in.web.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationEntryPoint authenticationEntryPoint;

	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
		return new JwtAuthenticationFilter(tokenProvider);
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint))
				.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
				.authorizeHttpRequests(auth -> auth
						// 1. Endpoint công khai (Public)
						.requestMatchers("/api/v1/auth/**").permitAll()
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers("/h2-console/**").permitAll()
						.requestMatchers("/accounts/send/**").permitAll()

						// 2. Endpoint bảo vệ (Protected - Yêu cầu JWT)
						.requestMatchers(HttpMethod.GET, "/api/v1/wallets/**").authenticated()
						.requestMatchers(HttpMethod.POST, "/api/v1/wallets/**").authenticated()
						.requestMatchers(HttpMethod.DELETE, "/api/v1/wallets/**").authenticated()

						.anyRequest().authenticated()
				)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
