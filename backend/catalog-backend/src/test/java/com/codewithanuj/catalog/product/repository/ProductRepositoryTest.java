package com.codewithanuj.catalog.product.repository;

import com.codewithanuj.catalog.product.model.Product;
import com.codewithanuj.catalog.product.model.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void saveAndFindByIdReturnsProduct() {
        Product product = new Product(
                "PRD-001",
                "Clay Mug",
                "Handmade clay mug",
                new BigDecimal("24.99"),
                "USD",
                ProductStatus.IN_STOCK,
                true,
                "https://instagram.com/p/001"
        );

        productRepository.save(product);

        Optional<Product> found = productRepository.findById("PRD-001");

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Clay Mug");
        assertThat(found.get().getStatus()).isEqualTo(ProductStatus.IN_STOCK);
        assertThat(found.get().getPrice()).isEqualByComparingTo("24.99");
    }

    @Test
    void findAllReturnsAllSavedProducts() {
        productRepository.save(new Product(
                "PRD-001", "Clay Mug", "Desc", new BigDecimal("24.99"),
                "USD", ProductStatus.IN_STOCK, true, "https://instagram.com/p/001"
        ));
        productRepository.save(new Product(
                "PRD-002", "Desk Lamp", "Desc", new BigDecimal("49.99"),
                "USD", ProductStatus.OUT_OF_STOCK, false, "https://instagram.com/p/002"
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
                "PRD-001", "Clay Mug", "Desc", new BigDecimal("24.99"),
                "USD", ProductStatus.IN_STOCK, true, "https://instagram.com/p/001"
        ));
        productRepository.save(new Product(
                "PRD-002", "Desk Lamp", "Desc", new BigDecimal("49.99"),
                "USD", ProductStatus.OUT_OF_STOCK, false, "https://instagram.com/p/002"
        ));

        List<Product> inStock = productRepository.findByStatus(ProductStatus.IN_STOCK);

        assertThat(inStock).hasSize(1);
        assertThat(inStock.get(0).getProductNumber()).isEqualTo("PRD-001");
    }
}
