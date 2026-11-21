package com.stock.stockcontrolbackend.repository;

import com.stock.stockcontrolbackend.entity.StockItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockItemRepository extends JpaRepository<StockItem, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT si FROM StockItem si WHERE si.product.id = :productId AND si.warehouse.id = :warehouseId")
    Optional<StockItem> findOrUpdate(@Param("productId") Long productId,
                                     @Param("warehouseId") Long warehouseId);

    @EntityGraph(attributePaths = {"product", "warehouse"})
    List<StockItem> findByWarehouseIdOrderByProductNameAsc(Long warehouseId);

    @EntityGraph(attributePaths = {"product", "warehouse"})
    List<StockItem> findByProductIdOrderByWarehouseNameAsc(Long productId);

    @Query("SELECT si FROM StockItem si JOIN FROM si.product JOIN FETCH si.warehouse " + "WHERE si.quantity <= si.minQuantity")
    List<StockItem> findLowStock();

    boolean existsByWarehouseIdAndQuantityGreaterThan(Long warehouseId, BigDecimal quantity);

    @Query("SELECT si.product.id AS productId, SUM(si.quantity) AS total " + "FROM StockItem si WHERE si.product.id IN :productIds GROUP BY si.product.id")
    List<ProductStockTotal> sumQuantityByProductsIds(@Param("productIds")Collection<Long> productIds);


}