package com.codewithanuj.catalog.order.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

/**
 * Issues order codes (LC-1000, LC-1001, …) from a Postgres sequence, the same way
 * {@code ProductNumberGenerator} issues product numbers.
 *
 * <p>A sequence rather than a random string because the code is quoted out loud and
 * typed into WhatsApp by hand: it has to be short, unambiguous and never reused. A
 * customer referring to "LC-1042" weeks later must still mean one specific order, even
 * if that order was cancelled.
 */
@Component
public class OrderCodeGenerator {

    @PersistenceContext
    private EntityManager entityManager;

    public String next() {
        Number value = (Number) entityManager
                .createNativeQuery("SELECT nextval('order_code_seq')")
                .getSingleResult();
        return String.format("LC-%04d", value.longValue());
    }
}
