package com.stock.stockcontrolbackend.service;

import com.stock.stockcontrolbackend.dto.ProductRequest;
import com.stock.stockcontrolbackend.dto.ProductResponse;
import com.stock.stockcontrolbackend.entity.Product;
import com.stock.stockcontrolbackend.exception.DuplicateResourceException;
import com.stock.stockcontrolbackend.exception.ResourceNotFoundException;
import com.stock.stockcontrolbackend.repository.ProductRepository;
import com.stock.stockcontrolbackend.repository.ProductStockTotal;
import com.stock.stockcontrolbackend.repository.StockItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final StockItemRepository stockItemRepository;

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll(String name) {
        List<Product> products = (name == null || name.isBlank())
                ? productRepository.findByIsActiveTrueOrderByNameAsc()
                : productRepository.findByIsActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(name.trim());
        return toResponses(products);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return  toResponses(List.of(getProduct(id))).get(0);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String code = request.code().trim();
        if (productRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Ya existe un producto con el código: " + code);
        }
        Product product = new Product();
        apply(request, product);
        return ProductResponse.from(productRepository.save(product), BigDecimal.ZERO);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getProduct(id);
        String code = request.code().trim();
        if (productRepository.existsByCodeAndIdNot(code, id)) {
            throw new DuplicateResourceException("Ya existe un producto con el código: " + code);
        }
        apply(request, product);
        return toResponses(List.of(productRepository.save(product))).get(0);
    }

    @Transactional
    public void deactivate(Long id) {
        Product product = getProduct(id);
        product.setIsActive(false);
        productRepository.save(product);
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: =id" + id));
    }

    private List<ProductResponse> toResponses(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }
        List<Long> ids = products.stream().map(Product::getId).toList();
        Map<Long, BigDecimal> totals = stockItemRepository.sumQuantityByProductsIds(ids).stream()
                .collect(Collectors.toMap(ProductStockTotal::getProductId, ProductStockTotal::getTotal));

        return products.stream()
                .map(product -> ProductResponse.from(product, totals.getOrDefault(product.getId(), BigDecimal.ZERO))).toList();
    }

    private void apply(ProductRequest request, Product product) {
        product.setCode(request.code().trim());
        product.setName(request.name().trim());
        product.setUnitPrice(request.unitPrice());
        product.setFinalPrice(request.finalPrice());
    }
}