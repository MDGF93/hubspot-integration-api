package org.marcosdgf.hubspotintegrationapi.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.marcosdgf.hubspotintegrationapi.client.HubSpotCrmClient;
import org.marcosdgf.hubspotintegrationapi.dto.request.ContactCreateRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/contacts")
@Tag(name = "Contacts", description = "Operações de Contato no HubSpot via Feign")
@RequiredArgsConstructor
public class ContactController {

    private static final String ERROR_KEY = "error";
    private static final String DETAILS_KEY = "details";

    private final HubSpotCrmClient hubSpotCrmClient;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<String> createContact(
            @Parameter(description = "Dados do contato a ser criado") @Valid @RequestBody final ContactCreateRequest contactRequest) {

        final Map<String, Object> requestBodyMap = getStringObjectMap(contactRequest);

        log.debug("--- Iniciando createContact via Feign ---");

        try {
            final String responseBody = this.hubSpotCrmClient.createContact(requestBodyMap);

            log.debug("HubSpot Create Contact Response (Feign): {}", responseBody);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseBody);

        } catch (final FeignException e) {
            log.error("Erro da API HubSpot (Feign): Status {} - Response Body: {}",
                    e.status(), e.contentUTF8(), e);
            String errorBody = e.contentUTF8();
            if (errorBody == null || errorBody.isEmpty()) {
                Map<String, String> fallbackError = Map.of(ERROR_KEY, "Erro Feign", DETAILS_KEY, "Status: " + e.status());
                errorBody = convertMapToJson(fallbackError);
            }
            final MediaType contentType = determineErrorContentType(e);
            return ResponseEntity.status(e.status())
                    .contentType(contentType)
                    .body(errorBody);

        } catch (final Exception e) {
            log.error("Erro inesperado ao criar contato via Feign.", e);
            final Map<String, String> errorBody = Map.of(ERROR_KEY, "Erro interno inesperado no servidor", DETAILS_KEY, e.getMessage());
            final String errorJson = convertMapToJson(errorBody);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorJson);
        } finally {
            log.debug("--- Finalizando createContact via Feign ---");
        }
    }

    private static Map<String, Object> getStringObjectMap(final ContactCreateRequest contactRequest) {
        final Map<String, String> properties = Map.of(
                "email", contactRequest.email(),
                "firstname", contactRequest.firstname(),
                "lastname", contactRequest.lastname(),
                "phone", contactRequest.phone() != null ? contactRequest.phone() : "",
                "website", contactRequest.website() != null ? contactRequest.website() : ""
        );
        return Map.of("properties", properties);
    }

    private String convertMapToJson(final Map<String, ?> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (final JsonProcessingException exception) {
            log.error("Failed to serialize map to JSON: {}", map, exception);
            return buildErrorJson(exception);
        }
    }

    private String buildErrorJson(final JsonProcessingException exception) {
        Map<String, String> errorResponse = new LinkedHashMap<>();
        errorResponse.put(ERROR_KEY, "Internal error generating JSON response");
        errorResponse.put(DETAILS_KEY, exception.getMessage());

        try {
            return objectMapper.writeValueAsString(errorResponse);
        } catch (final JsonProcessingException errorException) {
            return "{\"error\":\"Internal error generating JSON response\",\"details\":\"Failed to serialize error message\"}";
        }
    }

    private MediaType determineErrorContentType(final FeignException e) {
        try {
            final Map<String, java.util.Collection<String>> headers = e.responseHeaders();
            if (headers != null && headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
                String contentTypeHeader = headers.get(HttpHeaders.CONTENT_TYPE).iterator().next();
                return MediaType.parseMediaType(contentTypeHeader);
            }
        } catch (final Exception ex) {
            log.warn("Could not determine Content-Type from FeignException headers.", ex);
        }
        return MediaType.APPLICATION_JSON;
    }

}
