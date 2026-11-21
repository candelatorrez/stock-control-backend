package com.stock.stockcontrolbackend.repository;

import com.stock.stockcontrolbackend.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    List<Warehouse> findByIsActiveTrueOrderByNameAsc();

}
