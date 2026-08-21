package com.codewithanuj.catalog.shared.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class LocalStorageServiceTest {

    @TempDir
    Path uploads;

    private LocalStorageService storage;

    @BeforeEach
    void setUp() {
        storage = new LocalStorageService(uploads.toString(), new ImageProcessor());
        storage.init();
    }

    @Test
    void writesBothVariantsToDiskAndReturnsTheWebUrl() throws IOException {
        String url = storage.store(jpegUpload());

        assertThat(url).startsWith("/uploads/");
        String filename = url.substring("/uploads/".length());
        assertThat(uploads.resolve(filename)).exists();
        assertThat(uploads.resolve(filename.replace(".jpg", "-thumb.jpg"))).exists();
        assertThat(Files.list(uploads)).hasSize(2);
    }

    @Test
    void deleteRemovesTheThumbnailAlongsideTheWebImage() throws IOException {
        String url = storage.store(jpegUpload());

        storage.delete(url);

        assertThat(Files.list(uploads)).isEmpty();
    }

    /**
     * A traversal attempt must not reach outside the uploads root. Filenames are
     * server-generated UUIDs, so this can only come from a hand-crafted stored URL.
     */
    @Test
    void deleteRefusesToEscapeTheUploadsRoot() throws IOException {
        Path outside = uploads.getParent().resolve("do-not-delete.txt");
        Files.writeString(outside, "keep me");

        storage.delete("/uploads/../" + outside.getFileName());

        assertThat(outside).exists();
        Files.delete(outside);
    }

    @Test
    void deleteIgnoresExternalAndNullUrls() {
        assertThatCode(() -> {
            storage.delete("https://images.example.com/other.jpg");
            storage.delete(null);
        }).doesNotThrowAnyException();
    }

    private MockMultipartFile jpegUpload() throws IOException {
        BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return new MockMultipartFile("files", "saree.jpg", "image/jpeg", out.toByteArray());
    }
}
