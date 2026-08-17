package com.codewithanuj.catalog.shared.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Stores uploaded images in S3. Enabled with {@code app.storage=s3}.
 *
 * <p>Photos written by {@link LocalStorageService} live inside the container and are
 * wiped on every deploy; the whole point of this implementation is that they outlive
 * the compute, and stay put if the app moves between AWS and GCP.
 *
 * <p><b>Serving the images is deployment configuration, not application code.</b> The
 * URLs stored in the database are relative ({@code /uploads/<key>}) by default, so the
 * edge in front of the app has to route {@code /uploads/*} at the bucket. Point
 * {@code app.storage.s3.public-url-prefix} at a CDN origin instead if you would rather
 * store absolute URLs — at the cost of every existing row breaking if that domain
 * changes.
 */
@Service
@ConditionalOnProperty(name = "app.storage", havingValue = "s3")
public class S3StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    /** A year — filenames contain a UUID, so a stored object is never overwritten. */
    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    private final S3Client s3;
    private final ImageProcessor imageProcessor;
    private final String bucket;
    private final String keyPrefix;
    private final String urlPrefix;

    public S3StorageService(S3Client s3,
                            ImageProcessor imageProcessor,
                            @Value("${app.storage.s3.bucket}") String bucket,
                            @Value("${app.storage.s3.key-prefix:}") String keyPrefix,
                            @Value("${app.storage.s3.public-url-prefix:/uploads/}") String urlPrefix) {
        this.s3 = s3;
        this.imageProcessor = imageProcessor;
        this.bucket = bucket;
        this.keyPrefix = normalizeKeyPrefix(keyPrefix);
        this.urlPrefix = urlPrefix.endsWith("/") ? urlPrefix : urlPrefix + "/";
    }

    @Override
    public String store(MultipartFile file) {
        ImageVariants variants = imageProcessor.process(file);
        put(variants.web());
        put(variants.thumb());
        return urlPrefix + variants.web().filename();
    }

    @Override
    public void delete(String url) {
        deleteOne(url);
        deleteOne(ImageVariants.thumbUrlFor(url));
    }

    private void put(ImageVariants.ImageFile image) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(keyPrefix + image.filename())
                .contentType(image.contentType())
                .cacheControl(CACHE_CONTROL)
                .build();
        try {
            s3.putObject(request, RequestBody.fromBytes(image.bytes()));
        } catch (S3Exception e) {
            log.error("S3 upload failed for {}: {}", image.filename(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file", e);
        }
    }

    private void deleteOne(String url) {
        if (url == null || !url.startsWith(urlPrefix)) {
            return; // null, blank, or a URL we did not write — nothing we own to remove
        }
        String key = keyPrefix + url.substring(urlPrefix.length());
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            // Best effort, same contract as the local implementation: a failed cleanup
            // must never fail the product update that triggered it.
            log.warn("Failed to delete S3 object {}: {}", key, e.getMessage());
        }
    }

    private static String normalizeKeyPrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "";
        }
        String trimmed = prefix.startsWith("/") ? prefix.substring(1) : prefix;
        return trimmed.endsWith("/") ? trimmed : trimmed + "/";
    }
}
