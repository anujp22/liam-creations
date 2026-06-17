package com.codewithanuj.catalog.product.repository;

import com.codewithanuj.catalog.product.model.Product;
import com.codewithanuj.catalog.product.model.ProductCategory;
import com.codewithanuj.catalog.product.model.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void clearProducts() {
        productRepository.deleteAll();
    }

    @Test
    void saveAndFindByIdReturnsProduct() {
        Product product = new Product(
                "PRD-001", "Kanjeevaram Saree", "Handwoven silk saree",
                new BigDecimal("18500.00"), "INR", ProductStatus.IN_STOCK, true, null, ProductCategory.BRIDAL_SAREES
        );

        productRepository.save(product);

        Optional<Product> found = productRepository.findById("PRD-001");

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Kanjeevaram Saree");
        assertThat(found.get().getStatus()).isEqualTo(ProductStatus.IN_STOCK);
        assertThat(found.get().getPrice()).isEqualByComparingTo("18500.00");
    }

    @Test
    void findAllReturnsAllSavedProducts() {
        productRepository.save(new Product(
                "PRD-001", "Kanjeevaram Saree", "Desc",
                new BigDecimal("18500.00"), "INR", ProductStatus.IN_STOCK, true, null, ProductCategory.BRIDAL_SAREES
        ));
        productRepository.save(new Product(
                "PRD-002", "Haldi Package", "Desc",
                new BigDecimal("2499.00"), "INR", ProductStatus.OUT_OF_STOCK, false, null, ProductCategory.HALDI_MEHENDI
        ));

        List<Product> products = productRepository.findAll();

        assertThat(products).hasSize(2);
        assertThat(products).extracting(Product::getProductNumber)
                .containsExactlyInAnyOrder("PRD-001", "PRD-002");
    }

    @Test
    void findByIdReturnsEmptyWhenProductDoesNotExist() {
        Optional<Product> found = productRepository.findById("PRD-999");

        assertThat(found).isEmpty();
    }

    @Test
    void findByStatusReturnsOnlyProductsWithMatchingStatus() {
        productRepository.save(new Product(
                "PRD-001", "Kanjeevaram Saree", "Desc",
                new BigDecimal("18500.00"), "INR", ProductStatus.IN_STOCK, true, null, ProductCategory.BRIDAL_SAREES
        ));
        productRepository.save(new Product(
                "PRD-002", "Haldi Package", "Desc",
                new BigDecimal("2499.00"), "INR", ProductStatus.OUT_OF_STOCK, false, null, ProductCategory.HALDI_MEHENDI
        ));

        Page<Product> inStock = productRepository.findByStatus(ProductStatus.IN_STOCK, PageRequest.of(0, 20));

        assertThat(inStock.getTotalElements()).isEqualTo(1);
        assertThat(inStock.getContent().get(0).getProductNumber()).isEqualTo("PRD-001");
    }

    @Test
    void findByCategoryReturnsOnlyProductsWithMatchingCategory() {
        productRepository.save(new Product(
                "PRD-001", "Kanjeevaram Saree", "Desc",
                new BigDecimal("18500.00"), "INR", ProductStatus.IN_STOCK, true, null, ProductCategory.BRIDAL_SAREES
        ));
        productRepository.save(new Product(
                "PRD-002", "Haldi Package", "Desc",
                new BigDecimal("2499.00"), "INR", ProductStatus.IN_STOCK, false, null, ProductCategory.HALDI_MEHENDI
        ));

        Page<Product> sarees = productRepository.findByCategory(ProductCategory.BRIDAL_SAREES, PageRequest.of(0, 20));

        assertThat(sarees.getTotalElements()).isEqualTo(1);
        assertThat(sarees.getContent().get(0).getProductNumber()).isEqualTo("PRD-001");
    }

    @Test
    void findFilteredWithSearchTermMatchesTitleCaseInsensitive() {
        productRepository.save(new Product(
                "PRD-001", "Kanjeevaram Silk Saree", "Handwoven fabric",
                new BigDecimal("18500.00"), "INR", ProductStatus.IN_STOCK, true, null, ProductCategory.BRIDAL_SAREES
        ));
        productRepository.save(new Product(
                "PRD-002", "Haldi Package", "Turmeric paste set",
                new BigDecimal("2499.00"), "INR", ProductStatus.IN_STOCK, false, null, ProductCategory.HALDI_MEHENDI
        ));

        Page<Product> result = productRepository.findFiltered(null, null, "silk", PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getProductNumber()).isEqualTo("PRD-001");
    }

    @Test
    void findFilteredWithSearchTermMatchesDescription() {
        productRepository.save(new Product(
                "PRD-001", "Haldi Package", "Contains turmeric paste",
                new BigDecimal("2499.00"), "INR", ProductStatus.IN_STOCK, false, null, ProductCategory.HALDI_MEHENDI
        ));
        productRepository.save(new Product(
                "PRD-002", "Silver Coin", "Gold dipped coin",
                new BigDecimal("500.00"), "INR", ProductStatus.IN_STOCK, false, null, ProductCategory.SWEETS_GIFTS
        ));

        Page<Product> result = productRepository.findFiltered(null, null, "turmeric", PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getProductNumber()).isEqualTo("PRD-001");
    }

    @Test
    void findFilteredWithStatusFilterAndSearchReturnsMatch() {
        productRepository.save(new Product(
                "PRD-001", "Saree", "Desc",
                new BigDecimal("1000.00"), "INR", ProductStatus.IN_STOCK, false, null, ProductCategory.BRIDAL_SAREES
        ));
        productRepository.save(new Product(
                "PRD-002", "Lehenga", "Desc",
                new BigDecimal("2000.00"), "INR", ProductStatus.OUT_OF_STOCK, false, null, ProductCategory.BRIDAL_LEHENGAS
        ));

        Page<Product> result = productRepository.findFiltered(ProductStatus.IN_STOCK, null, "saree", PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getProductNumber()).isEqualTo("PRD-001");
    }

    @Test
    void findByStatusAndCategoryReturnsMatchingProducts() {
        productRepository.save(new Product(
                "PRD-001", "Kanjeevaram Saree", "Desc",
                new BigDecimal("18500.00"), "INR", ProductStatus.IN_STOCK, true, null, ProductCategory.BRIDAL_SAREES
        ));
        productRepository.save(new Product(
                "PRD-002", "Banarasi Saree", "Desc",
                new BigDecimal("22000.00"), "INR", ProductStatus.OUT_OF_STOCK, false, null, ProductCategory.BRIDAL_SAREES
        ));
        productRepository.save(new Product(
                "PRD-003", "Haldi Package", "Desc",
                new BigDecimal("2499.00"), "INR", ProductStatus.IN_STOCK, false, null, ProductCategory.HALDI_MEHENDI
        ));

        Page<Product> result = productRepository.findByStatusAndCategory(
                ProductStatus.IN_STOCK, ProductCategory.BRIDAL_SAREES, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getProductNumber()).isEqualTo("PRD-001");
    }
}
