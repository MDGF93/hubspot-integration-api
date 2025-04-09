package org.marcosdgf.hubspotintegrationapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class OpenApiConfig {

    private final Environment environment;

    @Bean
    public OpenAPI customOpenAPI() {
        log.info(">>> customOpenAPI() bean method is EXECUTING!");

        final Info apiInfo = new Info()
                .title("HubSpot Integration API")
                .version("0.0.1-SNAPSHOT")
                .description("API para o desafio de integração com HubSpot, implementando fluxo OAuth2, webhooks e criação de contatos.")
                .contact(new Contact().name("Marcos Dantas Guimarães Filho").email("marcosdgfilho@gmail.com"));

        final List<Server> servers = new ArrayList<>();
        final String redirectUri = environment.getProperty("HUBSPOT_REDIRECT_URI");
        log.info("Attempting to read HUBSPOT_REDIRECT_URI for OpenAPI config: {}", redirectUri);

        if (redirectUri != null && !redirectUri.isBlank()) {
            try {
                final URI fullUri = new URI(redirectUri);
                final String baseUrl = new URI(fullUri.getScheme(), null, fullUri.getHost(), fullUri.getPort(), null, null, null).toString();
                log.info("Successfully parsed base URL for dynamic server: {}", baseUrl);
                servers.add(new Server().url(baseUrl).description("Dynamic Public Server URL (via Tunnel)"));
            } catch (final URISyntaxException e) {
                log.error("Failed to parse HUBSPOT_REDIRECT_URI '{}' to extract base URL for OpenAPI servers.", redirectUri, e);
                servers.add(new Server().url("FAILED_TO_PARSE_TUNNEL_URL").description("Dynamic URL (Parsing Error)"));
            } catch (final Exception e) {
                log.error("Unexpected error processing HUBSPOT_REDIRECT_URI '{}'", redirectUri, e);
                servers.add(new Server().url("ERROR_PROCESSING_TUNNEL_URL").description("Dynamic URL (Processing Error)"));
            }
        } else {
            log.warn("HUBSPOT_REDIRECT_URI environment variable not found or empty. Adding placeholder.");
            servers.add(new Server().url("TUNNEL_URL_NOT_SET").description("Dynamic URL (Not Set - Check Entrypoint)"));
        }

        servers.add(new Server().url("http://localhost:8080").description("Local Development Server"));

        return new OpenAPI()
                .info(apiInfo)
                .servers(servers)
                .components(new Components());
    }

}
