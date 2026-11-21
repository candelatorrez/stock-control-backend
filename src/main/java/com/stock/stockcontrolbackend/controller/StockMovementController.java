package com.stock.stockcontrolbackend.controller;

import com.stock.stockcontrolbackend.dto.StockMovementRequest;
import com.stock.stockcontrolbackend.dto.StockMovementResponse;
import com.stock.stockcontrolbackend.service.StockMovementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock-movements")
@RequiredArgsConstructor
public class StockMovementController {

    private final StockMovementService stockMovementService;


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockMovementResponse registerMovement(@Valid @RequestBody StockMovementRequest request) {
        return stockMovementService.registerMovement(request);
    }

    @GetMapping
    public List<StockMovementResponse> findMovements(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId
    ) {
        if (productId != null) {
            return stockMovementService.findByProduct(productId);
        }
        if (warehouseId != null) {
            return stockMovementService.findByWarehouse(warehouseId);
        }
        throw new IllegalArgumentException("productId o warehouseId son requeridos para filtrar el historial");
    }
}