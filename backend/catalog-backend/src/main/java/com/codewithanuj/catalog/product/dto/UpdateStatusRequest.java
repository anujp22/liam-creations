package com.codewithanuj.catalog.product.dto;

import com.codewithanuj.catalog.product.model.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull ProductStatus status) {}
