package com.codewithanuj.catalog.order.controller;

import com.codewithanuj.catalog.order.dto.OrderItemResponseDto;
import com.codewithanuj.catalog.order.dto.OrderResponseDto;
import com.codewithanuj.catalog.order.model.OrderStatus;
import com.codewithanuj.catalog.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    private OrderResponseDto order(String code, OrderStatus status) {
        return new OrderResponseDto(UUID.randomUUID(), code, "Asha", "9876543210", null,
                "12 Rose Street", null, status, new BigDecimal("2000.00"), "INR",
                List.of(new OrderItemResponseDto("PRD-1", "Red Saree", new BigDecimal("1000.00"), 2,
                        new BigDecimal("2000.00"))),
                Instant.now(), Instant.now());
    }

    @Test
    void listsEveryOrderWhenNoStatusIsGiven() throws Exception {
        when(orderService.getOrders(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order("LC-1000", OrderStatus.NEW))));

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderCode").value("LC-1000"))
                .andExpect(jsonPath("$.content[0].customerName").value("Asha"));
    }

    @Test
    void passesTheStatusFilterThrough() throws Exception {
        when(orderService.getOrders(eq(OrderStatus.PAID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order("LC-1001", OrderStatus.PAID))));

        mockMvc.perform(get("/api/admin/orders").param("status", "PAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PAID"));

        verify(orderService).getOrders(eq(OrderStatus.PAID), any(Pageable.class));
    }

    @Test
    void looksAnOrderUpByTheCodeTheCustomerQuotes() throws Exception {
        when(orderService.getByCode("LC-1000")).thenReturn(order("LC-1000", OrderStatus.NEW));

        mockMvc.perform(get("/api/admin/orders/LC-1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Red Saree"));
    }

    @Test
    void returns404ForAnUnknownCode() throws Exception {
        when(orderService.getByCode("LC-9999"))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "Order not found: LC-9999"));

        mockMvc.perform(get("/api/admin/orders/LC-9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void movesAnOrderToANewStatus() throws Exception {
        when(orderService.updateStatus("LC-1000", OrderStatus.SHIPPED))
                .thenReturn(order("LC-1000", OrderStatus.SHIPPED));

        mockMvc.perform(patch("/api/admin/orders/LC-1000/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    void rejectsAStatusThatIsNotAKnownValue() throws Exception {
        mockMvc.perform(patch("/api/admin/orders/LC-1000/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REFUNDED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("DELIVERED")));
    }

    @Test
    void reportsTheCountOfNewOrdersForTheAdminBadge() throws Exception {
        when(orderService.countNew()).thenReturn(4L);

        mockMvc.perform(get("/api/admin/orders/new-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(4));
    }
}
