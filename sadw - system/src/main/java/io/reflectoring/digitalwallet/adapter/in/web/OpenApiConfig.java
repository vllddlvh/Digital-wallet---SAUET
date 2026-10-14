package io.reflectoring.digitalwallet.adapter.in.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI customOpenAPI() {
		final String securitySchemeName = "BearerAuth";

		return new OpenAPI()
				.info(new Info()
						.title("Digital Wallet API")
						.version("1.0.0")
						.description("REST API backend cho dịch vụ Ví Điện Tử (Digital Wallet). Hệ thống bảo mật qua JWT Bearer."))
				.addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
				.components(new Components()
						.addSecuritySchemes(securitySchemeName,
								new SecurityScheme()
										.name(securitySchemeName)
										.type(SecurityScheme.Type.HTTP)
										.scheme("bearer")
										.bearerFormat("JWT")
										.description("Dán mã JWT Token nhận được từ /api/v1/auth/login vào đây")));
	}
}
