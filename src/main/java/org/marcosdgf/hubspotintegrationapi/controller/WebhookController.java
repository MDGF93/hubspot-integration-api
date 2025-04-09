package org.marcosdgf.hubspotintegrationapi.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.marcosdgf.hubspotintegrationapi.dto.webhook.HubSpotEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/webhooks")
@Tag(name = "Webhooks", description = "Recebimento de Webhooks do HubSpot")
@RequiredArgsConstructor
public class WebhookController {

    private final ObjectMapper objectMapper;

    @Value("${hubspot.client.secret}")
    private String hubspotClientSecret;


    @PostMapping("/contacts")
    @Operation(summary = "Receber Webhook de Criação de Contato",
            description = "Endpoint para HubSpot notificar sobre novas criações de contato. Valida a assinatura da requisição.")
    @ApiResponse(responseCode = "200", description = "Webhook recebido e validado com sucesso.")
    @ApiResponse(responseCode = "400", description = "Erro ao processar o corpo do webhook.")
    @ApiResponse(responseCode = "401", description = "Assinatura inválida ou timestamp expirado.")
    public ResponseEntity<String> handleContactCreationWebhook(
            @RequestBody final String rawBody,
            @RequestHeader("X-HubSpot-Signature-v3") final String signature,
            @RequestHeader("X-HubSpot-Request-Timestamp") final Long timestamp,
            final HttpServletRequest request
    ) {

       log.info("Webhook Recebido! Timestamp: {}, Signature: {}", timestamp, signature);

        final long maxDelta = 5L * 60L * 1000L;
        if (System.currentTimeMillis() - timestamp > maxDelta) {
            log.error("Webhook timestamp inválido (muito antigo).");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Timestamp inválido");
        }

        if (!isValidSignatureV3(signature, timestamp, rawBody, request)) {
            log.error("Assinatura do Webhook inválida!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Assinatura inválida");
        }

        log.debug("Assinatura do Webhook validada com sucesso!");

        try {
            final List<HubSpotEvent> events = objectMapper.readValue(rawBody, new TypeReference<>() {});
            log.debug("Webhook contém {} evento(s).", events.size());

            for (HubSpotEvent event : events) {
                log.debug("Processando evento: subscriptionType={}, objectId={}", event.getSubscriptionType(), event.getObjectId());

                if ("contact.creation".equalsIgnoreCase(event.getSubscriptionType())) {
                    log.debug("Evento de CRIAÇÃO DE CONTATO recebido para o contato ID: {}", event.getObjectId());
                } else {
                    log.debug("Evento ignorado (tipo não é contact.creation): {}", event.getSubscriptionType());
                }
            }

        } catch (final IOException e) {
            log.error("Erro ao fazer parse do JSON do corpo do webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erro ao processar corpo do webhook: " + e.getMessage());
        } catch (final Exception e) {
            log.error("Erro inesperado ao processar eventos do webhook.", e);
        }


        return ResponseEntity.ok("Webhook recebido");
    }

    boolean isValidSignatureV3(final String signatureHeader,
                               final Long timestamp,
                               final String requestBody,
                               final HttpServletRequest request
    ) {
        String calculatedSignature = null;
        String sourceString = null;
        try {
            final String requestUri = getRequestUri(request);
            sourceString = request.getMethod() + requestUri + requestBody + timestamp;

            final SecretKeySpec secretKeySpec = new SecretKeySpec(hubspotClientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);
            final byte[] hash = mac.doFinal(sourceString.getBytes(StandardCharsets.UTF_8));
            calculatedSignature = Base64.getEncoder().encodeToString(hash);

            final boolean isValid = MessageDigest.isEqual(calculatedSignature.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8));

            if (!isValid) {
                log.error("Webhook Signature Validation FAILED!");
                log.error("Timestamp Received: {}", timestamp);
                log.error("Signature Header Received: {}", signatureHeader);
                log.error("Request URI Used: {}", requestUri);
                log.error("Request Method Used: {}", request.getMethod());
                log.error("Request Body Snippet (First 200 chars): {}", requestBody.substring(0, Math.min(requestBody.length(), 200)));
                log.error("Constructed Source String (Check for subtle differences): {}", sourceString);
                log.error("Calculated Signature: {}", calculatedSignature);
                return false;
            }

            return true;

        } catch (final Exception e) {
            log.error("Unexpected error during signature validation: {}", e.getMessage(), e);
            log.error("Details during unexpected exception: Timestamp={}, Header={}, SourceStringAttempt={}, CalculatedSigAttempt={}",
                    timestamp, signatureHeader, sourceString, calculatedSignature);
            return false;
        }
    }

    private String getRequestUri(HttpServletRequest request) {
        String requestUri = request.getRequestURL().toString();

        if (request.getQueryString() != null) {
            requestUri += "?" + request.getQueryString();
        }
        return requestUri;
    }

}
