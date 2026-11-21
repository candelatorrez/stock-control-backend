package com.stock.stockcontrolbackend.controller;

import com.stock.stockcontrolbackend.dto.StockItemResponse;
import com.stock.stockcontrolbackend.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping
    public List<StockItemResponse> findStock(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId
    ) {
        if (productId != null) {
            return stockService.findByProduct(productId);
        }
        if (warehouseId != null) {
            return stockService.findByWarehouse(warehouseId);
        }
        throw new IllegalArgumentException("productId o warehouseId son requeridos para consultar el stock");
    }

    @GetMapping("/low")
    public  List<StockItemResponse> findLowStock() {
        return stockService.findLowStock();
    }

}
