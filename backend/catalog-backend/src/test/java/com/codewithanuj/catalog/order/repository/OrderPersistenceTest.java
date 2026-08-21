package com.codewithanuj.catalog.order.repository;

import com.codewithanuj.catalog.order.model.CustomerOrder;
import com.codewithanuj.catalog.order.model.OrderItem;
import com.codewithanuj.catalog.order.model.OrderStatus;
import com.codewithanuj.catalog.order.service.OrderCodeGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The order schema and mapping against a real migration chain, rather than mocks:
 * V14 has to apply, the item cascade has to work, and the code sequence has to exist.
 */
@DataJpaTest
@Import(OrderCodeGenerator.class)
class OrderPersistenceTest {

    @Autowired
    private CustomerOrderRepository orderRepository;

    @Autowired
    private OrderCodeGenerator orderCodeGenerator;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private CustomerOrder order(String code) {
        CustomerOrder order = new CustomerOrder(code, "Asha", "9876543210", "asha@example.com",
                "12 Rose Street", "Deliver after 6pm", new BigDecimal("2500.00"), "INR");
        order.addItem(new OrderItem("PRD-1", "Red Saree", new BigDecimal("1000.00"), 2));
        order.addItem(new OrderItem("PRD-2", "Gold Drape", new BigDecimal("500.00"), 1));
        return order;
    }

    @Test
    void issuesSequentialOrderCodesStartingAtLc1000() {
        assertThat(orderCodeGenerator.next()).isEqualTo("LC-1000");
        assertThat(orderCodeGenerator.next()).isEqualTo("LC-1001");
    }

    @Test
    void savesAnOrderWithItsItemsInOneGo() {
        CustomerOrder saved = orderRepository.saveAndFlush(order("LC-2000"));
        entityManager.clear();

        CustomerOrder loaded = orderRepository.findByOrderCode("LC-2000").orElseThrow();
        assertThat(loaded.getItems()).hasSize(2);
        assertThat(loaded.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(loaded.getTotal()).isEqualByComparingTo("2500.00");
        assertThat(saved.getCreatedAt())
                .as("saveAndFlush must populate the creation timestamp before the DTO is built")
                .isNotNull();
    }

    @Test
    void keepsTheOrderLinesWhenTheProductIsDeletedFromTheCatalog() {
        // order_items deliberately has no foreign key to products. An order is a record
        // of what was agreed; permanently deleting a product must not rewrite history.
        orderRepository.saveAndFlush(order("LC-2001"));
        entityManager.clear();

        jdbcTemplate.update("DELETE FROM products WHERE product_number IN ('PRD-1', 'PRD-2')");

        assertThat(orderRepository.findByOrderCode("LC-2001").orElseThrow().getItems()).hasSize(2);
    }

    @Test
    void deletingAnOrderRemovesItsItems() {
        CustomerOrder saved = orderRepository.saveAndFlush(order("LC-2002"));
        entityManager.clear();

        orderRepository.deleteById(saved.getId());
        orderRepository.flush();

        Integer orphans = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_items WHERE order_id = ?", Integer.class, saved.getId());
        assertThat(orphans).isZero();
    }

    @Test
    void filtersByStatusNewestFirst() {
        orderRepository.saveAndFlush(order("LC-3000"));
        CustomerOrder paid = order("LC-3001");
        paid.setStatus(OrderStatus.PAID);
        orderRepository.saveAndFlush(paid);

        assertThat(orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.PAID, PageRequest.of(0, 10)))
                .extracting(CustomerOrder::getOrderCode)
                .containsExactly("LC-3001");
        assertThat(orderRepository.countByStatus(OrderStatus.NEW)).isEqualTo(1);
    }

    @Test
    void refusesTwoOrdersWithTheSameCode() {
        orderRepository.saveAndFlush(order("LC-4000"));

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> orderRepository.saveAndFlush(order("LC-4000")))
                .as("the order code is what a customer quotes; it has to identify one order")
                .isInstanceOf(Exception.class);
    }
}
