package com.codewithanuj.catalog.product.repository;

import com.codewithanuj.catalog.product.model.Product;
import com.codewithanuj.catalog.product.model.ProductCategory;
import com.codewithanuj.catalog.product.model.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, String> {

    // Active (non-deleted) listings used by the storefront and admin product list.
    Page<Product> findByDeletedFalse(Pageable pageable);
    Page<Product> findByStatusAndDeletedFalse(ProductStatus status, Pageable pageable);
    Page<Product> findByCategoryAndDeletedFalse(ProductCategory category, Pageable pageable);
    Page<Product> findByStatusAndCategoryAndDeletedFalse(ProductStatus status, ProductCategory category, Pageable pageable);

    // Soft-deleted products, for the admin Deleted tab.
    Page<Product> findByDeletedTrue(Pageable pageable);

    // On-sale products for the storefront Sale section.
    Page<Product> findBySalePriceIsNotNullAndDeletedFalse(Pageable pageable);

    // Inventory metrics (active = not deleted).
    long countByDeletedFalse();
    long countByDeletedTrue();
    long countByStatusAndDeletedFalse(ProductStatus status);
    long countByCategoryAndDeletedFalse(ProductCategory category);
    long countByFeaturedTrueAndDeletedFalse();
    long countBySalePriceIsNotNullAndDeletedFalse();

    // search is guaranteed non-null when called — callers must never pass null here
    @Query("SELECT p FROM Product p WHERE p.deleted = false AND " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> findFiltered(
            @Param("status") ProductStatus status,
            @Param("category") ProductCategory category,
            @Param("search") String search,
            Pageable pageable);
}
