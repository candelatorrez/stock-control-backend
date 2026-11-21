package com.stock.stockcontrolbackend.dto;

import com.stock.stockcontrolbackend.entity.MovementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class StockMovementRequest {

    @NotNull
    private Long productId;

    @NotNull
    private Long warehouseId;

    // Obligatorio solo cuando el type = TRANSFERENCIA
    private Long warehouseDestinationId;

    @NotNull
    private MovementType type;

    @NotNull
    @DecimalMin(value = "0.001",  message = "La cantidad debe ser mayor a 0")
    private BigDecimal quantity;

    private String reason;

    private String referenceDocument;

    private String createdBy;

}
