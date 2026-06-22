package com.codewithanuj.catalog.product.controller;

import com.codewithanuj.catalog.product.dto.MetricsResponseDto;
import com.codewithanuj.catalog.product.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminMetricsController {

    private final ProductService productService;

    public AdminMetricsController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/metrics")
    public MetricsResponseDto getMetrics() {
        return productService.getMetrics();
    }
}
