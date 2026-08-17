package com.codewithanuj.catalog.shared.storage;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Stores uploaded images on the local filesystem and serves them under /uploads/**.
 *
 * <p>Default for development. Files written here live inside the container and are lost
 * on every deploy, so production sets {@code app.storage=s3} to use
 * {@link S3StorageService} instead.
 */
@Service
@ConditionalOnProperty(name = "app.storage", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);
    private static final String URL_PREFIX = "/uploads/";

    private final Path root;
    private final ImageProcessor imageProcessor;

    public LocalStorageService(@Value("${app.uploads.dir:uploads}") String uploadsDir,
                               ImageProcessor imageProcessor) {
        this.root = Paths.get(uploadsDir).toAbsolutePath().normalize();
        this.imageProcessor = imageProcessor;
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create uploads directory: " + root, e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        ImageVariants variants = imageProcessor.process(file);
        write(variants.web());
        write(variants.thumb());
        return URL_PREFIX + variants.web().filename();
    }

    @Override
    public void delete(String url) {
        deleteOne(url);
        deleteOne(ImageVariants.thumbUrlFor(url));
    }

    private void write(ImageVariants.ImageFile image) {
        Path target = resolveInsideRoot(image.filename());
        if (target == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path");
        }
        try {
            Files.write(target, image.bytes());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file", e);
        }
    }

    private void deleteOne(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) {
            return; // null, blank, or an external URL — nothing we own to remove
        }
        Path target = resolveInsideRoot(url.substring(URL_PREFIX.length()));
        if (target == null) {
            log.warn("Refusing to delete file outside uploads root: {}", url);
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Failed to delete upload {}: {}", url, e.getMessage());
        }
    }

    /** Resolves a filename under the uploads root, or null if it would escape it. */
    private Path resolveInsideRoot(String filename) {
        Path target = root.resolve(filename).normalize();
        return target.startsWith(root) ? target : null;
    }
}
