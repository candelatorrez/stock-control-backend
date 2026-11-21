package com.stock.stockcontrolbackend.repository;


import com.stock.stockcontrolbackend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    List<Product> findByIsActiveTrueOrderByNameAsc();

    List<Product> findByIsActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(String name);
}