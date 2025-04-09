package org.marcosdgf.hubspotintegrationapi.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.marcosdgf.hubspotintegrationapi.client.HubSpotOAuthClient;
import org.marcosdgf.hubspotintegrationapi.dto.response.HubSpotTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class InMemoryTokenStorageService implements TokenStorageService {

    private final HubSpotOAuthClient hubSpotOAuthClient;

    @Value("${hubspot.client.id}")
    private String hubspotClientId;

    @Value("${hubspot.client.secret}")
    private String hubspotClientSecret;

    private String accessToken;
    private String refreshToken;
    private long expiresAtMillis;

    private static final long EXPIRY_MARGIN_MILLIS = Duration.ofMinutes(5).toMillis();

    @Override
    public synchronized void storeTokens(final HubSpotTokenResponse tokenResponse) {
        this.accessToken = tokenResponse.accessToken();
        if (tokenResponse.refreshToken() != null) {
            this.refreshToken = tokenResponse.refreshToken();
        }
        this.expiresAtMillis = System.currentTimeMillis() + (tokenResponse.expiresIn() * 1000L);

        log.debug("Tokens stored in memory. Access token expires around: {}",
                new java.util.Date(this.expiresAtMillis));
    }

    @Override
    public synchronized String getAccessToken() {
        if (isTokenInvalidOrExpired()) {
            log.warn("Access token is null or expired (or nearing expiry). Attempting refresh.");

            if (this.refreshToken == null) {
                log.error("Refresh token is null. Cannot refresh access token. Re-authentication required.");
                invalidateTokens();
                return null;
            }

            try {
                final HubSpotTokenResponse refreshedTokenResponse = attemptTokenRefreshWithFeign();
                if (refreshedTokenResponse != null) {
                    log.info("Token refreshed successfully via Feign.");
                    storeTokens(refreshedTokenResponse);
                    return this.accessToken;
                } else {
                    log.error("Token refresh attempt via Feign failed, response was null.");
                    invalidateTokens();
                    return null;
                }
            } catch (Exception e) {
                log.error("Exception occurred during token refresh process: {}", e.getMessage(), e);
                invalidateTokens();
                return null;
            }
        }

        log.debug("Returning valid access token.");
        return this.accessToken;
    }

    private boolean isTokenInvalidOrExpired() {
        return this.accessToken == null || System.currentTimeMillis() >= (this.expiresAtMillis - EXPIRY_MARGIN_MILLIS);
    }

    private HubSpotTokenResponse attemptTokenRefreshWithFeign() {
        log.debug("Attempting to refresh HubSpot token using Feign client.");

        try {
            return this.hubSpotOAuthClient.refreshToken(
                    "refresh_token",
                    hubspotClientId,
                    hubspotClientSecret,
                    this.refreshToken
            );
        } catch (final FeignException e) {
            log.error("Error during token refresh from HubSpot API via Feign: Status {}, Body {}",
                    e.status(), e.contentUTF8(), e);
            if (e.status() >= 400 && e.status() < 500) {
                log.error("Client error {} during refresh, likely invalid refresh token or client credentials. Re-authentication required.", e.status());
                invalidateTokens();
            }
            return null;
        } catch (final Exception e) {
            log.error("Unexpected error during Feign token refresh call: {}", e.getMessage(), e);
            return null;
        }
    }

    private void invalidateTokens() {
        log.warn("Invalidating stored tokens due to refresh failure or client error.");
        this.accessToken = null;
        this.refreshToken = null;
        this.expiresAtMillis = 0;
    }

}
