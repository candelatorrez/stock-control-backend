package com.stock.stockcontrolbackend.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(

        @NotBlank(message = "El código es obligatorio")
        @Size(max = 50, message = "El código no puede superar los 50 carácteres")
        String code,

        @NotBlank(message = "El nombre del producto es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 carácteres")
        String name,

        @NotNull(message = "El precio unitario es obligatorio")
        @PositiveOrZero(message = "El precio unitario no puede ser negativo")
        @Digits(integer = 10,  fraction = 2, message = "El precio unitario admite hasta 10 enteros y 2 decimales")
        BigDecimal unitPrice,

        @NotNull(message = "El precio final es obligatorio")
        @PositiveOrZero(message = "El precio final no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio final admite hasta 10 enteros y 2 decimales")
        BigDecimal finalPrice
) {
}
