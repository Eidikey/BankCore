package com.bankcore.shared.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Base OpenAPI configuration (TECHNICAL-DESIGN #41).
 *
 * <p>Exposes the contract at {@code /v3/api-docs} and Swagger UI at
 * {@code /swagger-ui.html}. Controllers document their own operations, DTOs and
 * the {@code Idempotency-Key} header where applicable; this class only provides
 * the shared metadata and the JWT {@code bearerAuth} scheme referenced by
 * protected operations.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH_SCHEME = "bearerAuth";

    @Bean
    OpenAPI bankCoreOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BankCore API")
                        .version("1.0")
                        .description("""
                                Simulated banking API (MVP). Base currency MXN.
                                All routes are versioned under /api/v1.
                                Financial operations accept an Idempotency-Key header.
                                Error responses follow {timestamp, status, code, message, path}.""")
                        .contact(new Contact().name("BankCore Team"))
                        .license(new License().name("Educational use")))
                .components(new Components().addSecuritySchemes(
                        BEARER_AUTH_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token (15 min). Send as: Authorization: Bearer <token>")));
    }
}
