package com.codewithanuj.catalog.product.controller;

import com.codewithanuj.catalog.product.dto.ProductResponseDto;
import com.codewithanuj.catalog.product.model.ProductStatus;
import com.codewithanuj.catalog.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(ProductControllerTest.TestConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StubProductService productService;

    @Test
    void getProductsReturnsAllProductsWhenNoStatusIsProvided() throws Exception {
        productService.allProducts = List.of(
                createProduct("PRD-001", ProductStatus.IN_STOCK),
                createProduct("PRD-002", ProductStatus.OUT_OF_STOCK)
        );

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].productNumber").value("PRD-001"))
                .andExpect(jsonPath("$[1].productNumber").value("PRD-002"));
    }

    @Test
    void getProductsReturnsFilteredProductsWhenStatusIsProvided() throws Exception {
        productService.filteredProducts = List.of(
                createProduct("PRD-001", ProductStatus.IN_STOCK)
        );

        mockMvc.perform(get("/api/products")
                        .param("status", "IN_STOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productNumber").value("PRD-001"))
                .andExpect(jsonPath("$[0].status").value("IN_STOCK"));
    }

    @Test
    void getProductsReturnsBadRequestWhenStatusIsInvalid() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(contentTypeJson())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid value for 'status': INVALID_STATUS"))
                .andExpect(jsonPath("$.path").value("/api/products"));
    }

    @Test
    void getProductReturnsSingleProductWhenProductNumberExists() throws Exception {
        productService.singleProduct = Optional.of(
                createProduct("PRD-001", ProductStatus.IN_STOCK)
        );

        mockMvc.perform(get("/api/products/PRD-001"))
                .andExpect(status().isOk())
                .andExpect(contentTypeJson())
                .andExpect(jsonPath("$.productNumber").value("PRD-001"))
                .andExpect(jsonPath("$.title").value("Sample Product PRD-001"));
    }

    @Test
    void getProductReturnsNotFoundWhenProductDoesNotExist() throws Exception {
        productService.singleProduct = Optional.empty();

        mockMvc.perform(get("/api/products/PRD-999"))
                .andExpect(status().isNotFound())
                .andExpect(contentTypeJson())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product not found: PRD-999"))
                .andExpect(jsonPath("$.path").value("/api/products/PRD-999"));
    }

    private ProductResponseDto createProduct(String productNumber, ProductStatus status) {
        return new ProductResponseDto(
                productNumber,
                "Sample Product " + productNumber,
                "Simple test product",
                new BigDecimal("19.99"),
                "USD",
                status,
                true,
                "https://instagram.com/p/" + productNumber.toLowerCase()
        );
    }

    private static org.springframework.test.web.servlet.ResultMatcher contentTypeJson() {
        return org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        StubProductService stubProductService() {
            return new StubProductService();
        }
    }

    static class StubProductService extends ProductService {

        private List<ProductResponseDto> allProducts = new ArrayList<>();
        private List<ProductResponseDto> filteredProducts = new ArrayList<>();
        private Optional<ProductResponseDto> singleProduct = Optional.empty();

        @Override
        public List<ProductResponseDto> getAllProducts() {
            return allProducts;
        }

        @Override
        public List<ProductResponseDto> getProductsByStatus(ProductStatus status) {
            return filteredProducts;
        }

        @Override
        public Optional<ProductResponseDto> getProductByProductNumber(String productNumber) {
            return singleProduct;
        }
    }
}
