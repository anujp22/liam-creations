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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    private static final String VALID = """
            {
              "items": [{"productNumber": "PRD-1", "quantity": 2}],
              "customerName": "Asha",
              "customerPhone": "9876543210",
              "customerEmail": "asha@example.com",
              "customerAddress": "12 Rose Street",
              "notes": "Deliver after 6pm"
            }
            """;

    private OrderResponseDto placed() {
        return new OrderResponseDto(UUID.randomUUID(), "LC-1000", "Asha", "9876543210",
                "asha@example.com", "12 Rose Street", "Deliver after 6pm", OrderStatus.NEW,
                new BigDecimal("2000.00"), "INR",
                List.of(new OrderItemResponseDto("PRD-1", "Red Saree", new BigDecimal("1000.00"), 2,
                        new BigDecimal("2000.00"))),
                Instant.now(), Instant.now());
    }

    @Test
    void returns201WithTheOrderCodeAndServerComputedTotal() throws Exception {
        when(orderService.placeOrder(any())).thenReturn(placed());

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderCode").value("LC-1000"))
                .andExpect(jsonPath("$.total").value(2000.00))
                .andExpect(jsonPath("$.items[0].unitPrice").value(1000.00));
    }

    @Test
    void rejectsAnOrderWithNoItems() throws Exception {
        String body = VALID.replace("""
                [{"productNumber": "PRD-1", "quantity": 2}]""", "[]");

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("at least one item")));

        verify(orderService, never()).placeOrder(any());
    }

    @Test
    void requiresANameAPhoneAndAnAddress() throws Exception {
        String body = """
                {
                  "items": [{"productNumber": "PRD-1", "quantity": 1}],
                  "customerName": "  ",
                  "customerPhone": "",
                  "customerAddress": ""
                }
                """;

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("name is required")))
                .andExpect(jsonPath("$.message").value(containsString("phone is required")))
                .andExpect(jsonPath("$.message").value(containsString("address is required")));
    }

    @Test
    void rejectsAMalformedEmailButAcceptsAMissingOne() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID.replace("asha@example.com", "not-an-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("valid address")));

        when(orderService.placeOrder(any())).thenReturn(placed());
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID.replace("\"asha@example.com\"", "null")))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsNonsensicalQuantities() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID.replace("\"quantity\": 2", "\"quantity\": 0")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID.replace("\"quantity\": 2", "\"quantity\": 500")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void capsTheNumberOfDistinctProductsInOneOrder() throws Exception {
        String manyItems = java.util.stream.IntStream.rangeClosed(1, 51)
                .mapToObj(i -> "{\"productNumber\": \"PRD-" + i + "\", \"quantity\": 1}")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID.replace("""
                                [{"productNumber": "PRD-1", "quantity": 2}]""", manyItems)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("at most 50")));
    }
}
