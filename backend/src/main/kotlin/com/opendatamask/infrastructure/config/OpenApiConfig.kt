package com.opendatamask.infrastructure.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.servers.Server
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "OpenDataMask API",
        version = "1.0",
        description = "Open-source data masking and anonymisation platform. " +
            "Mask, subset, and generate test data from your production databases.",
        contact = Contact(name = "OpenDataMask", url = "https://github.com/MaximumTrainer/OpenDataMask"),
        license = License(name = "MIT", url = "https://opensource.org/licenses/MIT")
    ),
    servers = [
        Server(url = "/", description = "Current server")
    ],
    security = [SecurityRequirement(name = "bearerAuth"), SecurityRequirement(name = "apiKey")]
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    `in` = SecuritySchemeIn.HEADER,
    description = "JWT token obtained from POST /api/auth/login"
)
@SecurityScheme(
    name = "apiKey",
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.HEADER,
    paramName = "X-API-Key",
    description = "API key for service account access"
)
class OpenApiConfig
