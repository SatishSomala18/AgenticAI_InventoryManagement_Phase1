package com.example.inventarymanagement.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@OpenAPIDefinition(info = @Info(title = "Inventory Management & Procurement API", version = "v1", description = "POC-07 full stack CRUD API for inventory and procurement flows. Key rules: SKU format SKU-{CATEGORY_PREFIX}-{NNNN}, PO format PO-{YEAR}-{NNNN}, low/out-of-stock alerts are persisted in alert domain, and PO transitions are strictly validated.", contact = @Contact(name = "Inventory Team", email = "support@example.com"), license = @License(name = "Internal Use")), servers = @Server(url = "/", description = "Default Server"), security = @SecurityRequirement(name = "bearerAuth"))
public class OpenApiConfig {
}
