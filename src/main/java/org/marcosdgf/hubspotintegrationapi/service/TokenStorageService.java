package org.marcosdgf.hubspotintegrationapi.service;

import org.marcosdgf.hubspotintegrationapi.dto.response.HubSpotTokenResponse;

public interface TokenStorageService {

    void storeTokens(final HubSpotTokenResponse tokenResponse);
    String getAccessToken();

}
