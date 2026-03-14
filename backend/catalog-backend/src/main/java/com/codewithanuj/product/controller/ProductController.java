package com.codewithanuj.product.controller;

import com.codewithanuj.product.dto.ProductResponseDto;
import com.codewithanuj.product.model.ProductStatus;
import com.codewithanuj.product.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/api/products")
    public List<ProductResponseDto> getProducts(@RequestParam(required = false) ProductStatus status) {
        if (status == null) {
            return productService.getAllProducts();
        }

        return productService.getProductsByStatus(status);
    }

    @GetMapping("/api/products/{productNumber}")
    public ProductResponseDto getProduct(@PathVariable String productNumber) {
        return productService.getProductByProductNumber(productNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found: " + productNumber
                ));
    }
}
