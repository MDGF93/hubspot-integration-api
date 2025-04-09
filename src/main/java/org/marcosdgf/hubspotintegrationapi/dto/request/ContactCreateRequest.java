package org.marcosdgf.hubspotintegrationapi.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ContactCreateRequest(
        @NotBlank(message = "Email é obrigatório") @Email(message = "Formato de email inválido") String email,
        @NotBlank(message = "Primeiro nome é obrigatório") String firstname,
        @NotBlank(message = "Sobrenome é obrigatório") String lastname, String phone, String website) {}
