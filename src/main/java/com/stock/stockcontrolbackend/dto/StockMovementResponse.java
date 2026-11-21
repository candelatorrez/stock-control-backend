package com.stock.stockcontrolbackend.dto;

import com.stock.stockcontrolbackend.entity.MovementType;
import com.stock.stockcontrolbackend.entity.StockMovement;
import com.stock.stockcontrolbackend.entity.Warehouse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockMovementResponse(
        Long id,
        Long productId,
        String productCode,
        String productName,
        Long warehouseId,
        String warehouseName,
        Long warehouseDestinationId,
        String warehouseDestinationName,
        MovementType  type,
        BigDecimal quantity,
        String reason,
        String referenceDocument,
        String createdBy,
        LocalDateTime createdAt
) {
    public static StockMovementResponse from(StockMovement movement) {
        Warehouse destination = movement.getWarehouseDestination();
        return new StockMovementResponse(
                movement.getId(),
                movement.getProduct().getId(),
                movement.getProduct().getCode(),
                movement.getProduct().getName(),
                movement.getWarehouse().getId(),
                movement.getWarehouse().getName(),
                destination != null ? destination.getId() : null,
                destination != null ? destination.getName() : null,
                movement.getType(),
                movement.getQuantity(),
                movement.getReason(),
                movement.getReferenceDocument(),
                movement.getCreatedBy(),
                movement.getCreatedAt()
        );
    }
}
