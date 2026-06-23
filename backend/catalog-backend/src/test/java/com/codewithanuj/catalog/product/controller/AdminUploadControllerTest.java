package com.codewithanuj.catalog.product.controller;

import com.codewithanuj.catalog.shared.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUploadController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorageService storageService;

    @Test
    void uploadStoresFilesAndReturnsUrls() throws Exception {
        when(storageService.store(any())).thenReturn("/uploads/abc.jpg");

        MockMultipartFile file = new MockMultipartFile(
                "files", "saree.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/admin/uploads").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urls[0]").value("/uploads/abc.jpg"));
    }
}
