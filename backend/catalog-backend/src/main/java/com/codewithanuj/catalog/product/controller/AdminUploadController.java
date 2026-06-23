package com.codewithanuj.catalog.product.controller;

import com.codewithanuj.catalog.shared.storage.StorageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/uploads")
public class AdminUploadController {

    private final StorageService storageService;

    public AdminUploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    /** Accepts one or more image files and returns their public URLs in order. */
    @PostMapping
    public Map<String, List<String>> upload(@RequestParam("files") List<MultipartFile> files) {
        List<String> urls = files.stream().map(storageService::store).toList();
        return Map.of("urls", urls);
    }
}
