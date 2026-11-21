package com.stock.stockcontrolbackend.dto;

import com.stock.stockcontrolbackend.entity.Product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String code,
        String name,
        BigDecimal unitPrice,
        BigDecimal finalPrice,
        BigDecimal totalQuantity,
        boolean active
) {

    public static ProductResponse from(Product product, BigDecimal totalQuantity) {
        return new ProductResponse(
                product.getId(),
                product.getCode(),
                product.getName(),
                product.getUnitPrice(),
                product.getFinalPrice(),
                totalQuantity,
                product.getIsActive()
        );
    }
}
