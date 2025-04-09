package org.marcosdgf.hubspotintegrationapi.controller;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marcosdgf.hubspotintegrationapi.client.HubSpotOAuthClient;
import org.marcosdgf.hubspotintegrationapi.dto.response.HubSpotTokenResponse;
import org.marcosdgf.hubspotintegrationapi.service.TokenStorageService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthControllerTest {

    @Mock
    private HubSpotOAuthClient hubSpotOAuthClient;

    @Mock
    private TokenStorageService tokenStorageService;

    @InjectMocks
    private OAuthController oAuthController;

    private final String mockClientId = "test-client-id";
    private final String mockClientSecret = "test-client-secret";
    private final String mockRedirectUri = "http://localhost:8080/oauth/callback";
    private final String mockScopes = "contacts,tickets,e-commerce";
    private final String mockAuthorizeUri = "https://app.hubspot.com/oauth/authorize";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(oAuthController, "hubspotClientId", mockClientId);
        ReflectionTestUtils.setField(oAuthController, "hubspotClientSecret", mockClientSecret);
        ReflectionTestUtils.setField(oAuthController, "hubspotRedirectUri", mockRedirectUri);
        ReflectionTestUtils.setField(oAuthController, "hubspotScopes", mockScopes);
        ReflectionTestUtils.setField(oAuthController, "hubspotAuthorizeUri", mockAuthorizeUri);
    }

    @Test
    void getAuthorizationUrl_ShouldReturnCorrectURL() { // Keep throws for URLDecoder
        final String expectedDecodedScopeValue = mockScopes.replace(",", " "); // "contacts tickets e-commerce"

        final ResponseEntity<Map<String, String>> response = oAuthController.getAuthorizationUrl();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final String authUrlString = response.getBody().get("authorizationUrl");
        assertNotNull(authUrlString);

        final UriComponents uriComponents = UriComponentsBuilder.fromUriString(authUrlString).build();

        final URI baseUri = URI.create(mockAuthorizeUri);
        assertEquals(baseUri.getScheme(), uriComponents.getScheme(), "Scheme mismatch");
        assertEquals(baseUri.getHost(), uriComponents.getHost(), "Host mismatch");
        assertEquals(baseUri.getPath(), uriComponents.getPath(), "Path mismatch");

        assertEquals(mockClientId, uriComponents.getQueryParams().getFirst("client_id"), "client_id mismatch");
        assertEquals("code", uriComponents.getQueryParams().getFirst("response_type"), "response_type mismatch");

        final String rawRedirectUri = uriComponents.getQueryParams().getFirst("redirect_uri");
        final String rawScope = uriComponents.getQueryParams().getFirst("scope");
        assertNotNull(rawRedirectUri, "Raw redirect_uri parameter missing");
        assertNotNull(rawScope, "Raw scope parameter missing");

        final String decodedRedirectUri = URLDecoder.decode(rawRedirectUri, StandardCharsets.UTF_8);
        final String decodedScope = URLDecoder.decode(rawScope, StandardCharsets.UTF_8);
        assertEquals(mockRedirectUri, decodedRedirectUri, "redirect_uri mismatch after manual decoding");
        assertEquals(expectedDecodedScopeValue, decodedScope, "scope mismatch after manual decoding");
    }

    @Test
    void handleCallback_Success() {
        final String mockCode = "test-authorization-code";
        final HubSpotTokenResponse mockTokenResponse = new HubSpotTokenResponse(
                "test-access-token",
                "test-refresh-token",
                3600,
                "Bearer"
        );

        when(hubSpotOAuthClient.exchangeCodeForToken(
                "authorization_code",
                mockClientId,
                mockClientSecret,
                mockRedirectUri,
                mockCode
        )).thenReturn(mockTokenResponse);

        final ResponseEntity<String> response = oAuthController.handleCallback(mockCode);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OAuth bem-sucedido! Token recebido e processado via Feign.", response.getBody());

        verify(tokenStorageService).storeTokens(mockTokenResponse);
    }

    @Test
    void handleCallback_ClientReturnsNull() {
        final String mockCode = "test-authorization-code";

        when(hubSpotOAuthClient.exchangeCodeForToken(
                anyString(), anyString(), anyString(), anyString(), anyString()
        )).thenReturn(null);

        final ResponseEntity<String> response = oAuthController.handleCallback(mockCode);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Erro ao processar resposta de token do HubSpot.", response.getBody());

        verify(tokenStorageService, never()).storeTokens(any());
    }

    @Test
    void handleCallback_FeignException() {
        final String mockCode = "test-authorization-code";
        final String errorJson = "{\"status\":\"error\",\"message\":\"Invalid code\"}";

        final Request request = Request.create(
                Request.HttpMethod.POST,
                "https://api.hubspot.com/oauth/v1/token",
                new HashMap<>(),
                null,
                new RequestTemplate()
        );

        final FeignException feignException = new FeignException.BadRequest(
                "Bad Request",
                request,
                errorJson.getBytes(StandardCharsets.UTF_8),
                null
        );

        when(hubSpotOAuthClient.exchangeCodeForToken(
                anyString(), anyString(), anyString(), anyString(), anyString()
        )).thenThrow(feignException);

        final ResponseEntity<String> response = oAuthController.handleCallback(mockCode);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Erro na comunicação com HubSpot (Feign)"));
    }

    @Test
    void handleCallback_GenericException() {
        final String mockCode = "test-authorization-code";
        final RuntimeException exception = new RuntimeException("Test error message");

        when(hubSpotOAuthClient.exchangeCodeForToken(
                anyString(), anyString(), anyString(), anyString(), anyString()
        )).thenThrow(exception);

        final ResponseEntity<String> response = oAuthController.handleCallback(mockCode);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Erro interno ao processar o callback OAuth: Test error message", response.getBody());

        verify(tokenStorageService, never()).storeTokens(any());
    }

}
