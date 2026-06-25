package com.codewithanuj.catalog.shared.exception;

import com.codewithanuj.catalog.product.dto.ProductCreateRequest;
import com.codewithanuj.catalog.product.service.ProductService;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import(ValidationExceptionHandlerTest.TestConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class ValidationExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    // ProductController is loaded by @WebMvcTest and requires ProductService
    @MockBean
    private ProductService productService;

    // AdminUploadController is also picked up by the broad @WebMvcTest scan
    @MockBean
    private com.codewithanuj.catalog.shared.storage.StorageService storageService;

    // Review controllers are picked up by the broad @WebMvcTest scan too
    @MockBean
    private com.codewithanuj.catalog.review.service.ReviewService reviewService;

    // SitemapController (broad scan) needs the product repository
    @MockBean
    private com.codewithanuj.catalog.product.repository.ProductRepository productRepository;

    // Registers TestController as a bean so Spring MVC can discover its @PostMapping
    @TestConfiguration
    static class TestConfig {
        @Bean
        public TestController testController() {
            return new TestController();
        }
    }

    // Test-only controller — triggers @Valid on ProductCreateRequest
    @RestController
    static class TestController {
        @PostMapping("/test/products")
        public String create(@Valid @RequestBody ProductCreateRequest request) {
            return "ok";
        }
    }

    @Test
    void returnsValidationErrorWhenTitleIsMissing() throws Exception {
        String body = """
                {
                  "description": "Nice mug",
                  "price": 24.99,
                  "currency": "USD",
                  "status": "IN_STOCK",
                  "featured": true,
                  "instagramPostUrl": "https://instagram.com/p/001"
                }
                """;

        mockMvc.perform(post("/test/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("title")));
    }

    @Test
    void returnsValidationErrorWhenPriceIsTooLow() throws Exception {
        String body = """
                {
                  "title": "Clay Mug",
                  "description": "Nice mug",
                  "price": 0.00,
                  "currency": "USD",
                  "status": "IN_STOCK",
                  "featured": true,
                  "instagramPostUrl": "https://instagram.com/p/001"
                }
                """;

        mockMvc.perform(post("/test/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("price must be greater than 0")));
    }
}
