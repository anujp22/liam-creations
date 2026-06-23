package com.codewithanuj.catalog.product.service;

import com.codewithanuj.catalog.product.dto.ProductCreateRequest;
import com.codewithanuj.catalog.product.dto.ProductResponseDto;
import com.codewithanuj.catalog.product.dto.ProductUpdateRequest;
import com.codewithanuj.catalog.product.dto.UpdateFeaturedRequest;
import com.codewithanuj.catalog.product.dto.UpdateStatusRequest;
import com.codewithanuj.catalog.product.model.Product;
import com.codewithanuj.catalog.product.model.ProductCategory;
import com.codewithanuj.catalog.product.model.ProductStatus;
import com.codewithanuj.catalog.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductNumberGenerator productNumberGenerator;

    @InjectMocks
    private ProductService productService;

    // ── Read methods ──────────────────────────────────────────────────────────

    @Test
    void getProductsWithNoFiltersReturnsMappedDtoList() {
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.findByDeletedFalse(pageable)).thenReturn(new PageImpl<>(List.of(
                product("PRD-001", ProductStatus.IN_STOCK),
                product("PRD-002", ProductStatus.OUT_OF_STOCK)
        )));

        Page<ProductResponseDto> result = productService.getProducts(null, null, null, false, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(ProductResponseDto::productNumber)
                .containsExactly("PRD-001", "PRD-002");
    }

    @Test
    void getProductsByStatusDelegatesToRepositoryFindByStatus() {
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.findByStatusAndDeletedFalse(ProductStatus.IN_STOCK, pageable))
                .thenReturn(new PageImpl<>(List.of(product("PRD-001", ProductStatus.IN_STOCK))));

        Page<ProductResponseDto> result = productService.getProducts(ProductStatus.IN_STOCK, null, null, false, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).status()).isEqualTo(ProductStatus.IN_STOCK);
    }

    @Test
    void getProductByProductNumberReturnsDtoWhenFound() {
        when(productRepository.findByProductNumberAndDeletedFalse("PRD-001"))
                .thenReturn(Optional.of(product("PRD-001", ProductStatus.IN_STOCK)));

        Optional<ProductResponseDto> result = productService.getProductByProductNumber("PRD-001");

        assertThat(result).isPresent();
        assertThat(result.get().productNumber()).isEqualTo("PRD-001");
    }

    @Test
    void getProductByProductNumberReturnsEmptyWhenNotFound() {
        when(productRepository.findByProductNumberAndDeletedFalse("PRD-999")).thenReturn(Optional.empty());

        Optional<ProductResponseDto> result = productService.getProductByProductNumber("PRD-999");

        assertThat(result).isEmpty();
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @Test
    void getProductsDelegatesToFindFiltered() {
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.findFiltered(null, null, "silk", pageable))
                .thenReturn(new PageImpl<>(List.of(product("PRD-001", ProductStatus.IN_STOCK))));

        Page<ProductResponseDto> result = productService.getProducts(null, null, "silk", false, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).productNumber()).isEqualTo("PRD-001");
    }

    @Test
    void getProductsNormalisesBlankSearchToNullAndFallsBackToFindAll() {
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.findByDeletedFalse(pageable))
                .thenReturn(new PageImpl<>(List.of(product("PRD-001", ProductStatus.IN_STOCK))));

        Page<ProductResponseDto> result = productService.getProducts(null, null, "   ", false, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ── Soft delete / restore / permanent delete ───────────────────────────────

    @Test
    void deleteProductSoftDeletesWhenProductExists() {
        Product existing = product("PRD-001", ProductStatus.IN_STOCK);
        when(productRepository.findById("PRD-001")).thenReturn(Optional.of(existing));

        productService.deleteProduct("PRD-001");

        assertThat(existing.isDeleted()).isTrue();
        verify(productRepository).save(existing);
    }

    @Test
    void deleteProductThrows404WhenProductNotFound() {
        when(productRepository.findById("PRD-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct("PRD-999"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Product not found: PRD-999");
    }

    @Test
    void restoreProductClearsDeletedFlag() {
        Product existing = product("PRD-001", ProductStatus.IN_STOCK);
        existing.setDeleted(true);
        when(productRepository.findById("PRD-001")).thenReturn(Optional.of(existing));
        when(productRepository.save(any())).thenReturn(existing);

        productService.restoreProduct("PRD-001");

        assertThat(existing.isDeleted()).isFalse();
        verify(productRepository).save(existing);
    }

    @Test
    void permanentlyDeleteProductCallsDeleteByIdWhenProductExists() {
        when(productRepository.existsById("PRD-001")).thenReturn(true);

        productService.permanentlyDeleteProduct("PRD-001");

        verify(productRepository).deleteById("PRD-001");
    }

    @Test
    void getDeletedProductsReturnsSoftDeletedRows() {
        Pageable pageable = PageRequest.of(0, 20);
        Product deleted = product("PRD-001", ProductStatus.IN_STOCK);
        deleted.setDeleted(true);
        when(productRepository.findByDeletedTrue(pageable))
                .thenReturn(new PageImpl<>(List.of(deleted)));

        Page<ProductResponseDto> result = productService.getDeletedProducts(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ── Write methods ─────────────────────────────────────────────────────────

    @Test
    void createProductAssignsGeneratedNumberSavesAndReturnsDto() {
        ProductCreateRequest request = new ProductCreateRequest(
                "Banarasi Silk Saree", "Hand-woven pure silk",
                new BigDecimal("8500.00"), "INR", ProductStatus.IN_STOCK, true, null, ProductCategory.BRIDAL_SAREES, null, null
        );

        when(productNumberGenerator.next()).thenReturn("PRD-010");
        when(productRepository.save(any())).thenReturn(product("PRD-010", ProductStatus.IN_STOCK));

        ProductResponseDto result = productService.createProduct(request);

        assertThat(result.productNumber()).isEqualTo("PRD-010");
    }

    @Test
    void updateProductSavesAndReturnsDto() {
        ProductUpdateRequest request = new ProductUpdateRequest(
                "Updated Saree", "Updated desc",
                new BigDecimal("9500.00"), "INR", ProductStatus.OUT_OF_STOCK, false, null, ProductCategory.BRIDAL_SAREES, null, null
        );

        when(productRepository.findById("PRD-001"))
                .thenReturn(Optional.of(product("PRD-001", ProductStatus.IN_STOCK)));
        when(productRepository.save(any())).thenReturn(product("PRD-001", ProductStatus.OUT_OF_STOCK));

        ProductResponseDto result = productService.updateProduct("PRD-001", request);

        assertThat(result.productNumber()).isEqualTo("PRD-001");
        assertThat(result.status()).isEqualTo(ProductStatus.OUT_OF_STOCK);
    }

    @Test
    void updateProductPreservesDeletedFlag() {
        ProductUpdateRequest request = new ProductUpdateRequest(
                "Updated Saree", "Updated desc",
                new BigDecimal("9500.00"), "INR", ProductStatus.OUT_OF_STOCK, false, null, ProductCategory.BRIDAL_SAREES, null, null
        );

        Product existing = product("PRD-001", ProductStatus.IN_STOCK);
        existing.setDeleted(true);
        when(productRepository.findById("PRD-001")).thenReturn(Optional.of(existing));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        productService.updateProduct("PRD-001", request);

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(saved.capture());
        assertThat(saved.getValue().isDeleted()).isTrue();
    }

    @Test
    void updateProductThrows404WhenProductNotFound() {
        ProductUpdateRequest request = new ProductUpdateRequest(
                "Ghost", "Missing",
                new BigDecimal("999.00"), "INR", ProductStatus.IN_STOCK, false, null, ProductCategory.WEDDING_DECOR, null, null
        );

        when(productRepository.findById("PRD-999")).thenReturn(Optional.empty());

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
                new BigDecimal("1999.00"), "INR", status, true, null, ProductCategory.WEDDING_DECOR
        );
    }
}
