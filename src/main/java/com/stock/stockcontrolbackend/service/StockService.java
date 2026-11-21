package com.stock.stockcontrolbackend.service;

import com.stock.stockcontrolbackend.dto.StockItemResponse;
import com.stock.stockcontrolbackend.repository.StockItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockItemRepository stockItemRepository;

    public List<StockItemResponse> findByWarehouse(Long warehouseId) {
        return stockItemRepository.findByWarehouseIdOrderByProductNameAsc(warehouseId).stream()
                .map(StockItemResponse::from)
                .toList();
    }

    public List<StockItemResponse> findByProduct(Long productId) {
        return stockItemRepository.findByProductIdOrderByWarehouseNameAsc(productId).stream()
                .map(StockItemResponse::from)
                .toList();
    }


    public List<StockItemResponse> findLowStock() {
        return stockItemRepository.findLowStock().stream()
                .map(StockItemResponse::from)
                .toList();
    }
}
