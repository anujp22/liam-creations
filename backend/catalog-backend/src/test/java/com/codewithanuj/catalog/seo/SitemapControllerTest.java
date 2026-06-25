package com.codewithanuj.catalog.seo;

import com.codewithanuj.catalog.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SitemapController.class)
@AutoConfigureMockMvc(addFilters = false)
class SitemapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductRepository productRepository;

    @Test
    void sitemapListsStaticPagesAndActiveProducts() throws Exception {
        when(productRepository.findActiveProductNumbers()).thenReturn(List.of("PRD-001", "PRD-002"));

        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<urlset")))
                .andExpect(content().string(containsString("http://localhost:5173/</loc>")))
                .andExpect(content().string(containsString("/sale</loc>")))
                .andExpect(content().string(containsString("/products/PRD-001</loc>")))
                .andExpect(content().string(containsString("/products/PRD-002</loc>")));
    }
}
