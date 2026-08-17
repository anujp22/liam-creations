package com.codewithanuj.catalog.shared.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class S3StorageServiceTest {

    private S3Client s3;
    private S3StorageService storage;

    @BeforeEach
    void setUp() {
        s3 = mock(S3Client.class);
        storage = new S3StorageService(s3, new ImageProcessor(), "catalog-photos", "products/", "/uploads/");
    }

    @Test
    void uploadsBothVariantsUnderTheKeyPrefixAndReturnsTheWebUrl() throws IOException {
        String url = storage.store(jpegUpload());

        ArgumentCaptor<PutObjectRequest> puts = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3, times(2)).putObject(puts.capture(), any(RequestBody.class));

        List<String> keys = puts.getAllValues().stream().map(PutObjectRequest::key).toList();
        assertThat(keys).allMatch(k -> k.startsWith("products/"));
        assertThat(keys).anyMatch(k -> k.contains(ImageVariants.THUMB_SUFFIX));

        assertThat(url).startsWith("/uploads/").doesNotContain(ImageVariants.THUMB_SUFFIX);
        // The returned URL must address the object we actually wrote.
        assertThat(keys).contains("products/" + url.substring("/uploads/".length()));
    }

    @Test
    void setsContentTypeAndALongCacheHeader() throws IOException {
        storage.store(jpegUpload());

        ArgumentCaptor<PutObjectRequest> puts = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3, times(2)).putObject(puts.capture(), any(RequestBody.class));

        assertThat(puts.getAllValues()).allSatisfy(request -> {
            assertThat(request.contentType()).isEqualTo("image/jpeg");
            assertThat(request.cacheControl()).contains("immutable");
        });
    }

    @Test
    void deleteRemovesTheThumbnailAlongsideTheWebImage() {
        storage.delete("/uploads/abc.jpg");

        ArgumentCaptor<DeleteObjectRequest> deletes = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3, times(2)).deleteObject(deletes.capture());

        assertThat(deletes.getAllValues().stream().map(DeleteObjectRequest::key))
                .containsExactlyInAnyOrder("products/abc.jpg", "products/abc-thumb.jpg");
    }

    @Test
    void deleteIgnoresUrlsWeDidNotWrite() {
        storage.delete("https://images.example.com/somebody-elses.jpg");
        storage.delete(null);

        verifyNoInteractions(s3);
    }

    /** Cleanup runs during a product update; a failure there must not fail the update. */
    @Test
    void deleteSwallowsS3ServiceErrors() {
        doThrow(S3Exception.builder().message("access denied").build())
                .when(s3).deleteObject(any(DeleteObjectRequest.class));

        assertThatCode(() -> storage.delete("/uploads/abc.jpg")).doesNotThrowAnyException();
    }

    /**
     * A timeout arrives as SdkClientException, which is a sibling of S3Exception rather
     * than a subtype — and is the likelier failure. Catching only S3Exception would let
     * this escape, breaking StorageService.delete's "never throws" contract.
     */
    @Test
    void deleteSwallowsNetworkFailuresToo() {
        doThrow(SdkClientException.create("connection reset"))
                .when(s3).deleteObject(any(DeleteObjectRequest.class));

        assertThatCode(() -> storage.delete("/uploads/abc.jpg")).doesNotThrowAnyException();
    }

    /** An upload failure, by contrast, must surface — as a clean 500, not a raw SDK error. */
    @Test
    void storeReportsNetworkFailuresAsAServerError() {
        doThrow(SdkClientException.create("connection reset"))
                .when(s3).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        assertThatThrownBy(() -> storage.store(jpegUpload()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private MockMultipartFile jpegUpload() throws IOException {
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return new MockMultipartFile("files", "saree.jpg", "image/jpeg", out.toByteArray());
    }
}
