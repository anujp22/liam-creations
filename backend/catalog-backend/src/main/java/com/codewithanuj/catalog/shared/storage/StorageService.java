package com.codewithanuj.catalog.shared.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Stores uploaded images and returns a public URL path for each.
 * Local-disk implementation now; an S3 implementation can be swapped in later
 * without touching callers.
 */
public interface StorageService {

    /** Persists the file and returns its public URL (e.g. "/uploads/<uuid>.jpg"). */
    String store(MultipartFile file);

    /**
     * Best-effort removal of a previously stored file by its public URL.
     * Never throws — a missing or external URL is simply ignored.
     */
    void delete(String url);
}
