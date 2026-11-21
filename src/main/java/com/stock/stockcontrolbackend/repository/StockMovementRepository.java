package com.stock.stockcontrolbackend.repository;

import com.stock.stockcontrolbackend.entity.StockMovement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement,  Long> {

    @EntityGraph(attributePaths = {"product", "warehouse", "warehouseDestination"})
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

    @EntityGraph(attributePaths = {"product", "warehouse", "warehouseDestination"})
    @Query("SELECT m FROM StockMovement m "
            + "WHERE m.warehouse.id = :warehouseId OR m.warehouseDestination.id = :warehouseId "
            + "ORDER BY m.createdAt DESC"
    )
    List<StockMovement> findByWarehouseInvolved(@Param("warehouseId") Long warehouseId);
}
