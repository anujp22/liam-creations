package com.codewithanuj.catalog.product.repository;

import com.codewithanuj.catalog.product.model.Product;
import com.codewithanuj.catalog.product.model.ProductCategory;
import com.codewithanuj.catalog.product.model.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {

    // Active product numbers for the sitemap.
    @Query("SELECT p.productNumber FROM Product p WHERE p.deleted = false ORDER BY p.productNumber")
    List<String> findActiveProductNumbers();

    // Active (non-deleted) listings used by the storefront and admin product list.
    Page<Product> findByDeletedFalse(Pageable pageable);
    Page<Product> findByStatusAndDeletedFalse(ProductStatus status, Pageable pageable);
    Page<Product> findByCategoryAndDeletedFalse(ProductCategory category, Pageable pageable);
    Page<Product> findByStatusAndCategoryAndDeletedFalse(ProductStatus status, ProductCategory category, Pageable pageable);

    // Soft-deleted products, for the admin Deleted tab.
    Page<Product> findByDeletedTrue(Pageable pageable);

    // Single active product for the public storefront (soft-deleted ones must 404).
    Optional<Product> findByProductNumberAndDeletedFalse(String productNumber);

    // On-sale products for the storefront Sale section.
    Page<Product> findBySalePriceIsNotNullAndDeletedFalse(Pageable pageable);

    // Inventory metrics (active = not deleted).
    long countByDeletedFalse();
    long countByDeletedTrue();
    long countByStatusAndDeletedFalse(ProductStatus status);
    long countByCategoryAndDeletedFalse(ProductCategory category);
    long countByFeaturedTrueAndDeletedFalse();
    long countBySalePriceIsNotNullAndDeletedFalse();

    /**
     * Every storefront listing goes through here. All four filters are independent and
     * any combination is valid; pass null for status/category, false for onSale, and
     * <strong>empty string</strong> (never null) for search to leave one out.
     *
     * <p>Deliberately one query rather than a branch per combination — the previous
     * shape special-cased on-sale and silently dropped the other filters.
     *
     * <p>{@code search} must not be null. An empty string matches everything via
     * {@code LIKE '%%'}, which is why there is no {@code :search IS NULL} branch here:
     * Postgres cannot infer a type for an untyped null parameter and binds it as
     * {@code bytea}, so {@code LOWER(?)} fails at runtime with
     * "function lower(bytea) does not exist". H2 accepts it, so the test suite will not
     * catch a regression here — verify against Postgres.
     */
    @Query("SELECT p FROM Product p WHERE p.deleted = false AND " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:onSale = false OR p.salePrice IS NOT NULL) AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> findFiltered(
            @Param("status") ProductStatus status,
            @Param("category") ProductCategory category,
            @Param("search") String search,
            @Param("onSale") boolean onSale,
            Pageable pageable);
}
