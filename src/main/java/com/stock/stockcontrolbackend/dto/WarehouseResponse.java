package com.stock.stockcontrolbackend.dto;

import com.stock.stockcontrolbackend.entity.Warehouse;

public record WarehouseResponse(
        Long id,
        String name,
        String code,
        String address,
        boolean active
) {
    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getName(),
                warehouse.getCode(),
                warehouse.getAddress(),
                warehouse.getIsActive()
        );
    }
}
