package com.stock.stockcontrolbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WarehouseRequest(
        @NotBlank(message = "El nombre de la sucursal es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 carateres")
        String name,

        @NotBlank(message = "El código de la sucursal es obligatorio")
        @Size(max = 20, message = "El código no puede superar los 20 caracteres")
        String code,

        @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
        String address
) {
}
