package org.marcosdgf.hubspotintegrationapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.marcosdgf.hubspotintegrationapi.client.HubSpotCrmClient;
import org.marcosdgf.hubspotintegrationapi.dto.request.ContactCreateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContactController.class)
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HubSpotCrmClient hubSpotCrmClient;

    @Test
    @DisplayName("POST /contacts should create contact successfully")
    void createContact_whenValidRequest_shouldReturnCreated() throws Exception {
        final ContactCreateRequest request =
                new ContactCreateRequest("test@example.com", "Test", "User", "123456789", "example.com");
        final String hubspotSuccessResponse =
                "{\"id\":\"12345\",\"properties\":{\"email\":\"test@example.com\"},\"createdAt\":\"...\"}";
        when(hubSpotCrmClient.createContact(any(Map.class))).thenReturn(hubspotSuccessResponse);

        mockMvc.perform(post("/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("test-user").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(hubspotSuccessResponse));

        verify(hubSpotCrmClient).createContact(any(Map.class));
    }

    @Test
    @DisplayName("POST /contacts should handle validation errors")
    void createContact_whenInvalidRequest_shouldReturnBadRequest() throws Exception {
        final ContactCreateRequest request = new ContactCreateRequest("invalid-email", null, "User", null, null);

        mockMvc.perform(
                        post("/contacts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(user("test-user").roles("USER"))
                                .with(csrf())
                        )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /contacts should handle FeignException from HubSpot API")
    void createContact_whenHubspotApiError_shouldReturnErrorStatus() throws Exception {
        final ContactCreateRequest request =
                new ContactCreateRequest("duplicate@example.com", "Duplicate", "Contact", null, null);
        final String requestJson = objectMapper.writeValueAsString(request);
        final String hubspotErrorResponse =
                "{\"status\":\"error\",\"message\":\"Contact already exists.\",\"correlationId\":\"...\"}";

        final Request mockRequest = Request.create(
                Request.HttpMethod.POST, "url", Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        when(hubSpotCrmClient.createContact(any(Map.class)))
                .thenThrow(FeignException.errorStatus(
                        "createContact",
                        feign.Response.builder()
                                .status(409)
                                .reason("Conflict")
                                .request(mockRequest)
                                .headers(Map.of(
                                        HttpHeaders.CONTENT_TYPE,
                                        Collections.singletonList(MediaType.APPLICATION_JSON_VALUE)))
                                .body(hubspotErrorResponse, StandardCharsets.UTF_8)
                                .build()));

        mockMvc.perform(post("/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .with(user("test-user").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(hubspotErrorResponse));

        verify(hubSpotCrmClient).createContact(any(Map.class));
    }

}
