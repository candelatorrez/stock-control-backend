package com.stock.stockcontrolbackend.repository;

import com.stock.stockcontrolbackend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
