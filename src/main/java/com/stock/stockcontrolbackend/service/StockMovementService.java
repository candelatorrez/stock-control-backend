package com.stock.stockcontrolbackend.service;


import com.stock.stockcontrolbackend.dto.StockMovementRequest;
import com.stock.stockcontrolbackend.dto.StockMovementResponse;
import com.stock.stockcontrolbackend.entity.*;
import com.stock.stockcontrolbackend.exception.InsufficientStockException;
import com.stock.stockcontrolbackend.exception.ResourceNotFoundException;
import com.stock.stockcontrolbackend.repository.ProductRepository;
import com.stock.stockcontrolbackend.repository.StockItemRepository;
import com.stock.stockcontrolbackend.repository.StockMovementRepository;
import com.stock.stockcontrolbackend.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final StockItemRepository stockItemRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    @Transactional
    public StockMovementResponse registerMovement(StockMovementRequest request) {
        Product product = findActiveProduct(request.getProductId());
        Warehouse origin = findActiveWarehouse(request.getWarehouseId());
        Warehouse destination = request.getType() == MovementType.TRANSFERENCIA
                ? resolveDestination(request) : null;


        applyToStock(request, product, origin, destination);

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setWarehouse(origin);
        movement.setWarehouseDestination(destination);
        movement.setType(request.getType());
        movement.setQuantity(request.getQuantity());
        movement.setReason(request.getReason());
        movement.setReferenceDocument(request.getReferenceDocument());
        movement.setCreatedBy(request.getCreatedBy());
        movement.setCreatedAt(LocalDateTime.now());

        return StockMovementResponse.from(stockMovementRepository.save(movement));
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> findByProduct(Long productId) {
        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(StockMovementResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> findByWarehouse(Long warehouseId) {
        return stockMovementRepository.findByWarehouseInvolved(warehouseId).stream()
                .map(StockMovementResponse::from)
                .toList();
    }

    private void applyToStock(StockMovementRequest request, Product product, Warehouse origin, Warehouse destination) {
        BigDecimal quantity = request.getQuantity();
        switch (request.getType()) {
            case ENTRADA -> increaseStock(product, origin, quantity);
            case SALIDA -> decreaseStock(product, origin, quantity);
            case AJUSTE -> setStock(product, origin, quantity);
            case TRANSFERENCIA -> {
                decreaseStock(product, origin, quantity);
                increaseStock(product, destination, quantity);
            }
        }
    }

    private void increaseStock(Product product, Warehouse warehouse, BigDecimal quantity) {
        StockItem item = lockStockItem(product, warehouse);
        item.setQuantity(item.getQuantity().add(quantity));
        persist(item);
    }

    private void decreaseStock(Product product, Warehouse warehouse, BigDecimal quantity) {
        StockItem item = lockStockItem(product, warehouse);
        if (item.getQuantity().compareTo(quantity) < 0) {
            throw new InsufficientStockException(
                    "Stock insuficiente de " + product.getCode() + "en la sucursal " + warehouse.getName()
                            + " (disponible: " + item.getQuantity() + ", solicitado: " + quantity + ")");
        }
            item.setQuantity(item.getQuantity().subtract(quantity));
            persist(item);
        }

    private void setStock(Product product, Warehouse warehouse, BigDecimal quantity) {
        StockItem item = lockStockItem(product, warehouse);
        item.setQuantity(quantity);
        persist(item);
    }

    private StockItem lockStockItem(Product product, Warehouse warehouse) {
        return stockItemRepository.findOrUpdate(product.getId(), warehouse.getId())
                .orElseGet(() -> newStockItem(product, warehouse));
    }

    private StockItem newStockItem(Product product,  Warehouse warehouse) {
        StockItem item = new StockItem();
        item.setProduct(product);
        item.setWarehouse(warehouse);
        return item;
    }

    private void persist(StockItem item) {
        item.setUpdatedAt(LocalDateTime.now());
        stockItemRepository.save(item);
    }

    private Warehouse resolveDestination(StockMovementRequest request) {
        Long destinationId = request.getWarehouseDestinationId();
        if (destinationId == null) {
            throw  new IllegalArgumentException("La sucursal de destino es obligatoria para una transferencia");
        }
        if (destinationId.equals(request.getWarehouseId())) {
            throw new IllegalArgumentException("La sucursal de destino no puede ser la misma que de origen");
        }
        return findActiveWarehouse(destinationId);
    }

    private Product findActiveProduct(Long id) {
        return productRepository.findById(id)
                .filter(Product::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado o inactivo: id=" + id));
    }

    private Warehouse findActiveWarehouse(Long id) {
        return warehouseRepository.findById(id)
                .filter(Warehouse::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada o inactiva: id=" + id));
    }
}