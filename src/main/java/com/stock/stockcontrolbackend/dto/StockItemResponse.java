package com.stock.stockcontrolbackend.dto;

import com.stock.stockcontrolbackend.entity.StockItem;

import java.math.BigDecimal;

public record StockItemResponse(
        Long productId,
        String productCode,
        String productName,
        Long warehouseId,
        String warehouseName,
        BigDecimal quantity,
        BigDecimal minQuantity
){
    public static StockItemResponse from(StockItem item) {
        return new StockItemResponse(
                item.getProduct().getId(),
                item.getProduct().getCode(),
                item.getProduct().getName(),
                item.getWarehouse().getId(),
                item.getWarehouse().getName(),
                item.getQuantity(),
                item.getMinQuantity()
        );
    }
}
