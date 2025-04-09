package org.marcosdgf.hubspotintegrationapi.dto.response; // Ou seu pacote de DTOs

import com.fasterxml.jackson.annotation.JsonProperty;

public record HubSpotTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") int expiresIn,
        @JsonProperty("token_type") String tokenType) {}
