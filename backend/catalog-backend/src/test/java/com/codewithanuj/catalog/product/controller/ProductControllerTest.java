package com.codewithanuj.catalog.product.controller;

import com.codewithanuj.catalog.product.dto.ProductResponseDto;
import com.codewithanuj.catalog.product.model.ProductStatus;
import com.codewithanuj.catalog.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    void getProductsReturnsAllProductsWhenNoStatusIsProvided() throws Exception {
        var items = List.of(
                product("PRD-001", ProductStatus.IN_STOCK),
                product("PRD-002", ProductStatus.OUT_OF_STOCK)
        );
        when(productService.getAllProducts(any(Pageable.class)))
                .thenReturn(new PageImpl<>(items));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].productNumber").value("PRD-001"))
                .andExpect(jsonPath("$.content[1].productNumber").value("PRD-002"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getProductsReturnsFilteredProductsWhenStatusIsProvided() throws Exception {
        var items = List.of(product("PRD-001", ProductStatus.IN_STOCK));
        when(productService.getProductsByStatus(any(ProductStatus.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(items));

        mockMvc.perform(get("/api/products").param("status", "IN_STOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].productNumber").value("PRD-001"))
                .andExpect(jsonPath("$.content[0].status").value("IN_STOCK"));
    }

    @Test
    void getProductsReturnsBadRequestWhenStatusIsInvalid() throws Exception {
        mockMvc.perform(get("/api/products").param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid value for 'status': INVALID_STATUS"))
                .andExpect(jsonPath("$.path").value("/api/products"));
    }

    @Test
    void getProductsReturnsBadRequestWithErrorFieldWhenStatusIsInvalid() throws Exception {
        mockMvc.perform(get("/api/products").param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void getProductsReturnsAllProductsWhenStatusIsEmptyString() throws Exception {
        when(productService.getAllProducts(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product("PRD-001", ProductStatus.IN_STOCK))));

        mockMvc.perform(get("/api/products").param("status", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getProductReturnsSingleProductWhenProductNumberExists() throws Exception {
        when(productService.getProductByProductNumber("PRD-001"))
                .thenReturn(Optional.of(product("PRD-001", ProductStatus.IN_STOCK)));

        mockMvc.perform(get("/api/products/PRD-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productNumber").value("PRD-001"))
                .andExpect(jsonPath("$.title").value("Sample Product"));
    }

    @Test
    void getProductReturnsNotFoundWhenProductDoesNotExist() throws Exception {
        when(productService.getProductByProductNumber("PRD-999"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/products/PRD-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product not found: PRD-999"))
                .andExpect(jsonPath("$.path").value("/api/products/PRD-999"));
    }

    private ProductResponseDto product(String productNumber, ProductStatus status) {
        return new ProductResponseDto(
                productNumber, "Sample Product", "A product",
                new BigDecimal("19.99"), "USD", status, true,
                "https://instagram.com/p/" + productNumber.toLowerCase()
        );
    }

    private static org.springframework.test.web.servlet.ResultMatcher contentTypeJson() {
        return org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON);
    }
}
