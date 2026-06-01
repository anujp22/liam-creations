package com.codewithanuj.catalog.product.controller;

import com.codewithanuj.catalog.product.dto.ProductCreateRequest;
import com.codewithanuj.catalog.product.dto.ProductResponseDto;
import com.codewithanuj.catalog.product.dto.ProductUpdateRequest;
import com.codewithanuj.catalog.product.dto.UpdateFeaturedRequest;
import com.codewithanuj.catalog.product.dto.UpdateStatusRequest;
import com.codewithanuj.catalog.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponseDto createProduct(@Valid @RequestBody ProductCreateRequest request) {
        return productService.createProduct(request);
    }

    @PutMapping("/{productNumber}")
    public ProductResponseDto updateProduct(
            @PathVariable String productNumber,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        return productService.updateProduct(productNumber, request);
    }

    @PatchMapping("/{productNumber}/featured")
    public ProductResponseDto updateFeatured(
            @PathVariable String productNumber,
            @RequestBody UpdateFeaturedRequest request
    ) {
        return productService.updateFeatured(productNumber, request);
    }

    @PatchMapping("/{productNumber}/status")
    public ProductResponseDto updateStatus(
            @PathVariable String productNumber,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        return productService.updateStatus(productNumber, request);
    }
}
