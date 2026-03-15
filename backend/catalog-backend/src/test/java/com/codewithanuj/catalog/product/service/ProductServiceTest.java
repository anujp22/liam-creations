package com.codewithanuj.catalog.product.service;

import com.codewithanuj.catalog.product.dto.ProductResponseDto;
import com.codewithanuj.catalog.product.model.ProductStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProductServiceTest {

    private final ProductService productService = new ProductService();

    @Test
    void getAllProductsReturnsAllMockProducts() {
        List<ProductResponseDto> products = productService.getAllProducts();

        assertThat(products).hasSize(3);
        assertThat(products)
                .extracting(ProductResponseDto::productNumber)
                .containsExactly("PRD-001", "PRD-002", "PRD-003");
    }

    @Test
    void getProductsByStatusReturnsOnlyMatchingProducts() {
        List<ProductResponseDto> products = productService.getProductsByStatus(ProductStatus.IN_STOCK);

        assertThat(products).hasSize(1);
        assertThat(products.get(0).productNumber()).isEqualTo("PRD-001");
        assertThat(products.get(0).status()).isEqualTo(ProductStatus.IN_STOCK);
    }

    @Test
    void getProductByProductNumberReturnsMatchingProductIgnoringCase() {
        Optional<ProductResponseDto> product = productService.getProductByProductNumber("prd-001");

        assertThat(product).isPresent();
        assertThat(product.get().title()).isEqualTo("Handmade Clay Mug");
    }

    @Test
    void getProductByProductNumberReturnsEmptyWhenProductDoesNotExist() {
        Optional<ProductResponseDto> product = productService.getProductByProductNumber("PRD-999");

        assertThat(product).isEmpty();
    }
}
