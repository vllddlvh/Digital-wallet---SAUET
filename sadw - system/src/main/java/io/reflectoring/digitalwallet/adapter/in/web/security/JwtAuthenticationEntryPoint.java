package io.reflectoring.digitalwallet.adapter.in.web.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	@Override
	public void commence(HttpServletRequest request,
						 HttpServletResponse response,
						 AuthenticationException authException) throws IOException {
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.getOutputStream().write(
				"{\"success\":false,\"errorCode\":\"UNAUTHORIZED\",\"message\":\"Yêu cầu xác thực JWT hợp lệ\"}"
						.getBytes(StandardCharsets.UTF_8)
		);
	}
}
