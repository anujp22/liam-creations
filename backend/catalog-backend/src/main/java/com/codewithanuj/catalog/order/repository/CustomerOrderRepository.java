package com.codewithanuj.catalog.order.repository;

import com.codewithanuj.catalog.order.model.CustomerOrder;
import com.codewithanuj.catalog.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {

    /** Admin Orders screen: everything, newest first. */
    Page<CustomerOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Admin Orders screen filtered by status (the "New" queue, mostly). */
    Page<CustomerOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    /** Look-up by the code the customer quotes in WhatsApp. */
    Optional<CustomerOrder> findByOrderCode(String orderCode);

    /** Count for the admin summary badge. */
    long countByStatus(OrderStatus status);
}
