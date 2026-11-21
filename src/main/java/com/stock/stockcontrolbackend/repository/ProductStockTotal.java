package com.stock.stockcontrolbackend.repository;

import java.math.BigDecimal;

public interface ProductStockTotal {
    Long getProductId();
    BigDecimal getTotal();
}
