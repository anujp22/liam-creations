package com.codewithanuj.catalog.product.controller;

import com.codewithanuj.catalog.product.dto.ProductCreateRequest;
import com.codewithanuj.catalog.product.dto.ProductResponseDto;
import com.codewithanuj.catalog.product.dto.ProductUpdateRequest;
import com.codewithanuj.catalog.product.dto.UpdateFeaturedRequest;
import com.codewithanuj.catalog.product.dto.UpdateStatusRequest;
import com.codewithanuj.catalog.product.model.ProductStatus;
import com.codewithanuj.catalog.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminProductController.class)
@Import(AdminProductControllerTest.TestConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    // ── POST ─────────────────────────────────────────────────────────────────

    @Test
    void createProductReturns201WhenRequestIsValid() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "PRD-010", "Banarasi Silk Saree", "Hand-woven pure silk",
                new BigDecimal("8500.00"), "INR", ProductStatus.IN_STOCK, true
        );

        when(productService.createProduct(any())).thenReturn(toDto("PRD-010", ProductStatus.IN_STOCK));

        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productNumber").value("PRD-010"));
    }

    @Test
    void createProductReturns409WhenProductNumberAlreadyExists() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "PRD-001", "Duplicate", "Already exists",
                new BigDecimal("999.00"), "INR", ProductStatus.IN_STOCK, false
        );

        when(productService.createProduct(any()))
                .thenThrow(new ResponseStatusException(CONFLICT, "Product already exists: PRD-001"));

        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Product already exists: PRD-001"));
    }

    // ── PUT ──────────────────────────────────────────────────────────────────

    @Test
    void updateProductReturns200WhenProductExists() throws Exception {
        ProductUpdateRequest request = new ProductUpdateRequest(
                "Updated Saree", "Updated desc",
                new BigDecimal("9500.00"), "INR", ProductStatus.OUT_OF_STOCK, false
        );

        when(productService.updateProduct(eq("PRD-001"), any()))
                .thenReturn(toDto("PRD-001", ProductStatus.OUT_OF_STOCK));

        mockMvc.perform(put("/api/admin/products/PRD-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productNumber").value("PRD-001"));
    }

    @Test
    void updateProductReturns404WhenProductDoesNotExist() throws Exception {
        ProductUpdateRequest request = new ProductUpdateRequest(
                "Ghost Product", "Does not exist",
                new BigDecimal("999.00"), "INR", ProductStatus.IN_STOCK, false
        );

        when(productService.updateProduct(eq("PRD-999"), any()))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "Product not found: PRD-999"));

        mockMvc.perform(put("/api/admin/products/PRD-999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product not found: PRD-999"));
    }

    // ── PATCH featured ───────────────────────────────────────────────────────

    @Test
    void updateFeaturedReturns200WhenProductExists() throws Exception {
        UpdateFeaturedRequest request = new UpdateFeaturedRequest(false);

        when(productService.updateFeatured(eq("PRD-001"), any()))
                .thenReturn(toDto("PRD-001", ProductStatus.IN_STOCK));

        mockMvc.perform(patch("/api/admin/products/PRD-001/featured")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productNumber").value("PRD-001"));
    }

    @Test
    void updateFeaturedReturns404WhenProductDoesNotExist() throws Exception {
        UpdateFeaturedRequest request = new UpdateFeaturedRequest(true);

        when(productService.updateFeatured(eq("PRD-999"), any()))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "Product not found: PRD-999"));

        mockMvc.perform(patch("/api/admin/products/PRD-999/featured")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found: PRD-999"));
    }

    // ── PATCH status ─────────────────────────────────────────────────────────

    @Test
    void updateStatusReturns200WhenProductExists() throws Exception {
        UpdateStatusRequest request = new UpdateStatusRequest(ProductStatus.OUT_OF_STOCK);

        when(productService.updateStatus(eq("PRD-001"), any()))
                .thenReturn(toDto("PRD-001", ProductStatus.OUT_OF_STOCK));

        mockMvc.perform(patch("/api/admin/products/PRD-001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OUT_OF_STOCK"));
    }

    @Test
    void updateStatusReturns404WhenProductDoesNotExist() throws Exception {
        UpdateStatusRequest request = new UpdateStatusRequest(ProductStatus.IN_STOCK);

        when(productService.updateStatus(eq("PRD-999"), any()))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "Product not found: PRD-999"));

        mockMvc.perform(patch("/api/admin/products/PRD-999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found: PRD-999"));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ProductResponseDto toDto(String productNumber, ProductStatus status) {
        return new ProductResponseDto(
                productNumber, "Sample Product", "A product",
                new BigDecimal("1999.00"), "INR", status, true
        );
    }
}
