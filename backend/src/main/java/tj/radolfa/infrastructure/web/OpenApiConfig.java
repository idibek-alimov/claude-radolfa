package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) configuration.
 *
 * <p>Access Swagger UI at: <code>/swagger-ui.html</code></p>
 * <p>Access OpenAPI spec at: <code>/v3/api-docs</code></p>
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI radolfaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Radolfa API")
                        .description("""
                                Radolfa E-commerce API - Product catalog with ERP synchronization.

                                ## Authentication
                                This API uses JWT Bearer token authentication.

                                1. Call `POST /api/v1/auth/login` with your phone number
                                2. Check backend logs for OTP (in DEV mode)
                                3. Call `POST /api/v1/auth/verify` with phone + OTP
                                4. Use the returned token in the Authorize button above

                                ## Roles
                                - **USER**: View products, profile, wishlist
                                - **MANAGER**: Upload images, edit descriptions
                                - **SYSTEM**: ERP sync operations

                                ## Important
                                ERPNext is the source of truth for `price`, `name`, and `stock`.
                                These fields can only be modified via ERP sync (SYSTEM role).
                                """)
                        .version("0.1.0")
                        .contact(new Contact()
                                .name("Radolfa Team")
                                .email("dev@radolfa.tj"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://radolfa.tj")))
                .servers(List.of(
                        new Server().url("/").description("Current server")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token obtained from /api/v1/auth/verify")));
    }
}
