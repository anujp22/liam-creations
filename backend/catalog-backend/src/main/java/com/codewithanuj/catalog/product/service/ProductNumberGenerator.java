package com.codewithanuj.catalog.product.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

/**
 * Issues monotonically increasing product numbers (PRD-001, PRD-002, …) backed by
 * a Postgres sequence. Numbers are never reused — once issued, a number stays
 * reserved even if the product is later (soft-)deleted.
 */
@Component
public class ProductNumberGenerator {

    @PersistenceContext
    private EntityManager entityManager;

    public String next() {
        Number value = (Number) entityManager
                .createNativeQuery("SELECT nextval('product_number_seq')")
                .getSingleResult();
        return String.format("PRD-%03d", value.longValue());
    }
}
