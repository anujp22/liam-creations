package com.codewithanuj.catalog.product.controller;

import com.codewithanuj.catalog.product.dto.MetricsResponseDto;
import com.codewithanuj.catalog.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminMetricsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminMetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    void getMetricsReturnsInventorySnapshot() throws Exception {
        when(productService.getMetrics()).thenReturn(new MetricsResponseDto(
                7,
                Map.of("IN_STOCK", 5L, "OUT_OF_STOCK", 2L),
                Map.of("BRIDAL_SAREES", 4L, "WEDDING_DECOR", 3L),
                2, 1, 3
        ));

        mockMvc.perform(get("/api/admin/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActive").value(7))
                .andExpect(jsonPath("$.byStatus.IN_STOCK").value(5))
                .andExpect(jsonPath("$.byCategory.BRIDAL_SAREES").value(4))
                .andExpect(jsonPath("$.featured").value(2))
                .andExpect(jsonPath("$.onSale").value(1))
                .andExpect(jsonPath("$.deleted").value(3));
    }
}
