package com.stock.stockcontrolbackend.service;

import com.stock.stockcontrolbackend.dto.WarehouseRequest;
import com.stock.stockcontrolbackend.dto.WarehouseResponse;
import com.stock.stockcontrolbackend.entity.Warehouse;
import com.stock.stockcontrolbackend.exception.BusinessRuleException;
import com.stock.stockcontrolbackend.exception.DuplicateResourceException;
import com.stock.stockcontrolbackend.exception.ResourceNotFoundException;
import com.stock.stockcontrolbackend.repository.StockItemRepository;
import com.stock.stockcontrolbackend.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final StockItemRepository stockItemRepository;

    @Transactional(readOnly = true)
    public List<WarehouseResponse> findAll() {
        return warehouseRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(WarehouseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WarehouseResponse findById(Long id) {
        return WarehouseResponse.from(getWarehouse(id));
    }

    @Transactional
    public WarehouseResponse create(WarehouseRequest request) {
        String code = request.code().trim();
        if (warehouseRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Ya existe una sucursal con el código: " + code);
        }
        Warehouse warehouse = new Warehouse();
        warehouse.setCreatedAt(LocalDateTime.now());
        apply(request, warehouse);
        return WarehouseResponse.from(warehouseRepository.save(warehouse));
    }

    @Transactional
    public WarehouseResponse update(Long id, WarehouseRequest request) {
        Warehouse warehouse = getWarehouse(id);
        String code = request.code().trim();
        if (warehouseRepository.existsByCodeAndIdNot(code, id)) {
            throw new DuplicateResourceException("Ya existe una sucursal con el código: " + code);
        }
        apply(request, warehouse);
        return WarehouseResponse.from(warehouseRepository.save(warehouse));
    }

    @Transactional
    public void deactivate(Long id) {
        Warehouse warehouse = getWarehouse(id);
        if (stockItemRepository.existsByWarehouseIdAndQuantityGreaterThan(id, BigDecimal.ZERO)) {
            throw new BusinessRuleException(
                    "No se puede desactivar la sucursal porque todavía tiene stock. "
                            + "Transfiérelo o ajústalo primero.");
        }
        warehouse.setIsActive(false);
        warehouseRepository.save(warehouse);
    }

    private Warehouse getWarehouse(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
    }

    private void apply(WarehouseRequest request, Warehouse warehouse) {
        warehouse.setName(request.name().trim());
        warehouse.setCode(request.code().trim());
        warehouse.setAddress(request.address());
    }
}
