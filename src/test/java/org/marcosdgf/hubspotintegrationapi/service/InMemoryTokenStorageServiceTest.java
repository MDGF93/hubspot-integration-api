package org.marcosdgf.hubspotintegrationapi.service;

import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marcosdgf.hubspotintegrationapi.client.HubSpotOAuthClient;
import org.marcosdgf.hubspotintegrationapi.dto.response.HubSpotTokenResponse;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InMemoryTokenStorageServiceTest {

    @Mock
    private HubSpotOAuthClient hubSpotOAuthClient;

    @InjectMocks
    private InMemoryTokenStorageService tokenStorageService;

    private final String clientId = "test-client-id";
    private final String clientSecret = "test-client-secret";

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(tokenStorageService, "hubspotClientId", clientId);
        ReflectionTestUtils.setField(tokenStorageService, "hubspotClientSecret", clientSecret);

        ReflectionTestUtils.setField(tokenStorageService, "expiresAtMillis", 0L);
    }

    @Test
    @DisplayName("Should store tokens correctly")
    void storeTokens_shouldStoreTokens() {

        final HubSpotTokenResponse tokenResponse = new HubSpotTokenResponse("access123", "refresh456", 3600, "bearer");

        tokenStorageService.storeTokens(tokenResponse);

        assertThat(ReflectionTestUtils.getField(tokenStorageService, "accessToken")).isEqualTo("access123");
        assertThat(ReflectionTestUtils.getField(tokenStorageService, "refreshToken")).isEqualTo("refresh456");

        assertThat((Long) ReflectionTestUtils.getField(tokenStorageService, "expiresAtMillis"))
                .isNotNull()
                .isGreaterThan(System.currentTimeMillis());
    }

    @Test
    @DisplayName("Should return valid access token if not expired")
    void getAccessToken_whenValid_shouldReturnToken() {

        final HubSpotTokenResponse tokenResponse = new HubSpotTokenResponse("valid-access", "valid-refresh", 3600, "bearer");
        tokenStorageService.storeTokens(tokenResponse);

        final String accessToken = tokenStorageService.getAccessToken();

        assertThat(accessToken).isEqualTo("valid-access");
        verify(hubSpotOAuthClient, never()).refreshToken(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should refresh token if access token is null")
    void getAccessToken_whenNull_shouldRefreshToken() {

        ReflectionTestUtils.setField(tokenStorageService, "refreshToken", "old-refresh");
        ReflectionTestUtils.setField(tokenStorageService, "accessToken", null);
        final HubSpotTokenResponse refreshedTokenResponse = new HubSpotTokenResponse("new-access", "new-refresh", 3600, "bearer");
        when(hubSpotOAuthClient.refreshToken(
                eq("refresh_token"),
                eq(clientId),
                eq(clientSecret),
                eq("old-refresh")))
                .thenReturn(refreshedTokenResponse);

        final String accessToken = tokenStorageService.getAccessToken();

        assertThat(accessToken).isEqualTo("new-access");
        verify(hubSpotOAuthClient).refreshToken(eq("refresh_token"), eq(clientId), eq(clientSecret), eq("old-refresh"));
        assertThat(ReflectionTestUtils.getField(tokenStorageService, "refreshToken")).isEqualTo("new-refresh");
    }

    @Test
    @DisplayName("Should refresh token if access token is expired (or near expiry)")
    void getAccessToken_whenExpired_shouldRefreshToken() {

        final HubSpotTokenResponse expiredToken = new HubSpotTokenResponse("expired-access", "refresh-to-use", 1, "bearer");
        tokenStorageService.storeTokens(expiredToken);

        ReflectionTestUtils.setField(tokenStorageService, "expiresAtMillis", System.currentTimeMillis() - 10000L);

        final HubSpotTokenResponse refreshedTokenResponse = new HubSpotTokenResponse("refreshed-access", "refreshed-refresh", 3600, "bearer");
        when(hubSpotOAuthClient.refreshToken(
                eq("refresh_token"),
                eq(clientId),
                eq(clientSecret),
                eq("refresh-to-use")))
                .thenReturn(refreshedTokenResponse);

        final String accessToken = tokenStorageService.getAccessToken();

        assertThat(accessToken).isEqualTo("refreshed-access");
        verify(hubSpotOAuthClient).refreshToken(eq("refresh_token"), eq(clientId), eq(clientSecret), eq("refresh-to-use"));
    }

    @Test
    @DisplayName("Should return null and invalidate tokens if refresh fails (FeignException)")
    void getAccessToken_whenRefreshFailsWithFeign_shouldReturnNullAndInvalidate() {

        ReflectionTestUtils.setField(tokenStorageService, "refreshToken", "bad-refresh");
        ReflectionTestUtils.setField(tokenStorageService, "accessToken", null);

        final Request mockRequest = Request.create(Request.HttpMethod.POST, "url", Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        when(hubSpotOAuthClient.refreshToken(anyString(), anyString(), anyString(), eq("bad-refresh")))
                .thenThrow(FeignException.errorStatus("refreshToken",
                        feign.Response.builder()
                                .status(400)
                                .reason("Bad Request")
                                .request(mockRequest)
                                .body("Invalid refresh token", StandardCharsets.UTF_8)
                                .build()));

        final String accessToken = tokenStorageService.getAccessToken();

        assertThat(accessToken).isNull();
        verify(hubSpotOAuthClient).refreshToken(anyString(), anyString(), anyString(), eq("bad-refresh"));

        assertThat(ReflectionTestUtils.getField(tokenStorageService, "accessToken")).isNull();
        assertThat(ReflectionTestUtils.getField(tokenStorageService, "refreshToken")).isNull();

        assertThat((Long) ReflectionTestUtils.getField(tokenStorageService, "expiresAtMillis")).isZero();
    }

    @Test
    @DisplayName("Should return null if refresh is needed but no refresh token exists")
    void getAccessToken_whenExpiredAndNoRefreshToken_shouldReturnNull() {

        ReflectionTestUtils.setField(tokenStorageService, "accessToken", null);
        ReflectionTestUtils.setField(tokenStorageService, "refreshToken", null);

        final String accessToken = tokenStorageService.getAccessToken();

        assertThat(accessToken).isNull();
        verify(hubSpotOAuthClient, never()).refreshToken(anyString(), anyString(), anyString(), anyString());
    }

}
