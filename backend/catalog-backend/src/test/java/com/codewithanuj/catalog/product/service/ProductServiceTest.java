package com.codewithanuj.catalog.product.service;

import com.codewithanuj.catalog.product.dto.ProductCreateRequest;
import com.codewithanuj.catalog.product.dto.ProductResponseDto;
import com.codewithanuj.catalog.product.dto.ProductUpdateRequest;
import com.codewithanuj.catalog.product.dto.UpdateFeaturedRequest;
import com.codewithanuj.catalog.product.dto.UpdateStatusRequest;
import com.codewithanuj.catalog.product.model.Product;
import com.codewithanuj.catalog.product.model.ProductStatus;
import com.codewithanuj.catalog.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    // ── Read methods ──────────────────────────────────────────────────────────

    @Test
    void getAllProductsReturnsMappedDtoList() {
        when(productRepository.findAll()).thenReturn(List.of(
                product("PRD-001", ProductStatus.IN_STOCK),
                product("PRD-002", ProductStatus.OUT_OF_STOCK)
        ));

        List<ProductResponseDto> result = productService.getAllProducts();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProductResponseDto::productNumber)
                .containsExactly("PRD-001", "PRD-002");
    }

    @Test
    void getProductsByStatusDelegatesToRepositoryFindByStatus() {
        when(productRepository.findByStatus(ProductStatus.IN_STOCK))
                .thenReturn(List.of(product("PRD-001", ProductStatus.IN_STOCK)));

        List<ProductResponseDto> result = productService.getProductsByStatus(ProductStatus.IN_STOCK);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(ProductStatus.IN_STOCK);
    }

    @Test
    void getProductByProductNumberReturnsDtoWhenFound() {
        when(productRepository.findById("PRD-001"))
                .thenReturn(Optional.of(product("PRD-001", ProductStatus.IN_STOCK)));

        Optional<ProductResponseDto> result = productService.getProductByProductNumber("PRD-001");

        assertThat(result).isPresent();
        assertThat(result.get().productNumber()).isEqualTo("PRD-001");
    }

    @Test
    void getProductByProductNumberReturnsEmptyWhenNotFound() {
        when(productRepository.findById("PRD-999")).thenReturn(Optional.empty());

        Optional<ProductResponseDto> result = productService.getProductByProductNumber("PRD-999");

        assertThat(result).isEmpty();
    }

    // ── Write methods ─────────────────────────────────────────────────────────

    @Test
    void createProductSavesAndReturnsDto() {
        ProductCreateRequest request = new ProductCreateRequest(
                "PRD-010", "New Mug", "A nice mug",
                new BigDecimal("29.99"), "USD", ProductStatus.IN_STOCK, true,
                "https://instagram.com/p/010"
        );

        when(productRepository.existsById("PRD-010")).thenReturn(false);
        when(productRepository.save(any())).thenReturn(product("PRD-010", ProductStatus.IN_STOCK));

        ProductResponseDto result = productService.createProduct(request);

        assertThat(result.productNumber()).isEqualTo("PRD-010");
    }

    @Test
    void createProductThrows409WhenProductAlreadyExists() {
        ProductCreateRequest request = new ProductCreateRequest(
                "PRD-001", "Duplicate", "Already exists",
                new BigDecimal("9.99"), "USD", ProductStatus.IN_STOCK, false,
                "https://instagram.com/p/001"
        );

        when(productRepository.existsById("PRD-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Product already exists: PRD-001");
    }

    @Test
    void updateProductSavesAndReturnsDto() {
        ProductUpdateRequest request = new ProductUpdateRequest(
                "Updated Mug", "Updated desc",
                new BigDecimal("34.99"), "USD", ProductStatus.OUT_OF_STOCK, false,
                "https://instagram.com/p/001"
        );

        when(productRepository.existsById("PRD-001")).thenReturn(true);
        when(productRepository.save(any())).thenReturn(product("PRD-001", ProductStatus.OUT_OF_STOCK));

        ProductResponseDto result = productService.updateProduct("PRD-001", request);

        assertThat(result.productNumber()).isEqualTo("PRD-001");
        assertThat(result.status()).isEqualTo(ProductStatus.OUT_OF_STOCK);
    }

    @Test
    void updateProductThrows404WhenProductNotFound() {
        ProductUpdateRequest request = new ProductUpdateRequest(
                "Ghost", "Missing",
                new BigDecimal("9.99"), "USD", ProductStatus.IN_STOCK, false,
                "https://instagram.com/p/999"
        );

        when(productRepository.existsById("PRD-999")).thenReturn(false);

        assertThatThrownBy(() -> productService.updateProduct("PRD-999", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Product not found: PRD-999");
    }

    @Test
    void updateFeaturedSavesAndReturnsDto() {
        when(productRepository.findById("PRD-001"))
                .thenReturn(Optional.of(product("PRD-001", ProductStatus.IN_STOCK)));
        when(productRepository.save(any())).thenReturn(product("PRD-001", ProductStatus.IN_STOCK));

        ProductResponseDto result = productService.updateFeatured("PRD-001", new UpdateFeaturedRequest(false));

        assertThat(result.productNumber()).isEqualTo("PRD-001");
    }

    @Test
    void updateFeaturedThrows404WhenProductNotFound() {
        when(productRepository.findById("PRD-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateFeatured("PRD-999", new UpdateFeaturedRequest(true)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Product not found: PRD-999");
    }

    @Test
    void updateStatusSavesAndReturnsDto() {
        when(productRepository.findById("PRD-001"))
                .thenReturn(Optional.of(product("PRD-001", ProductStatus.IN_STOCK)));
        when(productRepository.save(any())).thenReturn(product("PRD-001", ProductStatus.OUT_OF_STOCK));

        ProductResponseDto result = productService.updateStatus("PRD-001",
                new UpdateStatusRequest(ProductStatus.OUT_OF_STOCK));

        assertThat(result.productNumber()).isEqualTo("PRD-001");
    }

    @Test
    void updateStatusThrows404WhenProductNotFound() {
        when(productRepository.findById("PRD-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateStatus("PRD-999",
                new UpdateStatusRequest(ProductStatus.IN_STOCK)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Product not found: PRD-999");
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private Product product(String productNumber, ProductStatus status) {
        return new Product(
                productNumber, "Sample Product", "A product",
                new BigDecimal("19.99"), "USD", status, true,
                "https://instagram.com/p/" + productNumber.toLowerCase()
        );
    }
}
