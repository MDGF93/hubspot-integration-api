package org.marcosdgf.hubspotintegrationapi.controller;

import feign.FeignException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.marcosdgf.hubspotintegrationapi.client.HubSpotOAuthClient;
import org.marcosdgf.hubspotintegrationapi.dto.response.HubSpotTokenResponse;
import org.marcosdgf.hubspotintegrationapi.service.TokenStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/oauth")
@Tag(name = "OAuth", description = "Endpoints para o fluxo de Autenticação OAuth 2.0 com HubSpot")
@RequiredArgsConstructor
public class OAuthController {

    private final HubSpotOAuthClient hubSpotOAuthClient;
    private final TokenStorageService tokenStorageService;

    @Value("${hubspot.client.secret}")
    private String hubspotClientSecret;
    @Value("${hubspot.client.id}")
    private String hubspotClientId;
    @Value("${hubspot.redirect.uri}")
    private String hubspotRedirectUri;
    @Value("${hubspot.scopes}")
    private String hubspotScopes;
    @Value("${hubspot.oauth.authorizeUri}")
    private String hubspotAuthorizeUri;

    @GetMapping("/authorize")
    @Operation(summary = "Obter URL de Autorização HubSpot",
            description = "Gera a URL para redirecionar o usuário ao HubSpot e iniciar o fluxo OAuth 2.0.")
    @ApiResponse(responseCode = "200", description = "URL de autorização gerada com sucesso")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl() {

        final String authorizationUrl = UriComponentsBuilder
                .fromUriString(hubspotAuthorizeUri)
                .queryParam("client_id", hubspotClientId)
                .queryParam("redirect_uri", hubspotRedirectUri)
                .queryParam("scope", hubspotScopes.replace(",", " ")) // HubSpot usa espaços na URL
                .queryParam("response_type", "code")
                .toUriString();

        final Map<String, String> response = Map.of("authorizationUrl", authorizationUrl);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/callback")
    public ResponseEntity<String> handleCallback(@RequestParam final String code) {
        log.debug("Authorization Code Recebido: {}", code);

        try {
            final HubSpotTokenResponse tokenResponse = this.hubSpotOAuthClient.exchangeCodeForToken(
                    "authorization_code",
                    hubspotClientId,
                    hubspotClientSecret,
                    hubspotRedirectUri,
                    code
            );

            if (tokenResponse != null) {
                log.debug("Access Token: {}", tokenResponse.accessToken());
                log.debug("Refresh Token: {}", tokenResponse.refreshToken());
                tokenStorageService.storeTokens(tokenResponse);
                return ResponseEntity.ok("OAuth bem-sucedido! Token recebido e processado via Feign.");
            } else {
                log.error("Resposta de token recebida via Feign, mas objeto é nulo.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao processar resposta de token do HubSpot.");
            }

        } catch (final FeignException e) {
            log.error("Erro Feign ao trocar código por token: Status {} - Response Body: {}",
                    e.status(), e.contentUTF8(), e);
            return ResponseEntity.status(e.status())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("Erro na comunicação com HubSpot (Feign): " + e.contentUTF8());
        } catch (final Exception e) {
            log.error("Erro inesperado ao trocar código por token: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno ao processar o callback OAuth: " + e.getMessage());
        }
    }

}
