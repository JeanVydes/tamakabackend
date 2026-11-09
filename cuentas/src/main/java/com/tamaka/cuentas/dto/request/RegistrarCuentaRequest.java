package com.tamaka.cuentas.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrarCuentaRequest(

        @NotBlank
        @Email
        @Size(max = 180)
        String email,

        @NotBlank
        @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres")
        String password,

        @NotBlank
        @Size(max = 120)
        String nombre
) {
}
