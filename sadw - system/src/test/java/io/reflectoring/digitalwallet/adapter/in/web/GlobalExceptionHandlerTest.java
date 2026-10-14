package io.reflectoring.digitalwallet.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void handleIllegalArgument() {
		IllegalArgumentException ex = new IllegalArgumentException("Tham số không hợp lệ");
		ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgument(ex);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().isSuccess()).isFalse();
		assertThat(response.getBody().getErrorCode()).isEqualTo("BAD_REQUEST");
		assertThat(response.getBody().getMessage()).isEqualTo("Tham số không hợp lệ");
	}

	@Test
	void handleIllegalState() {
		IllegalStateException ex = new IllegalStateException("Trạng thái xung đột");
		ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalState(ex);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().isSuccess()).isFalse();
		assertThat(response.getBody().getErrorCode()).isEqualTo("CONFLICT");
		assertThat(response.getBody().getMessage()).isEqualTo("Trạng thái xung đột");
	}
}
