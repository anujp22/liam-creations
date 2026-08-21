package com.codewithanuj.catalog.shared.storage;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageProcessorTest {

    private final ImageProcessor processor = new ImageProcessor();

    @Test
    void downsizesALargePhotoToWebAndThumbnail() throws IOException {
        // A noisy 4000x3000 image, so JPEG cannot compress it into the budget for free.
        byte[] photo = noisyJpeg(4000, 3000);

        ImageVariants variants = processor.process(jpegUpload(photo));

        BufferedImage web = decode(variants.web().bytes());
        assertThat(Math.max(web.getWidth(), web.getHeight())).isEqualTo(1600);
        assertThat(web.getHeight()).isEqualTo(1200); // aspect ratio preserved
        assertThat(variants.web().bytes().length).isLessThan(photo.length);

        BufferedImage thumb = decode(variants.thumb().bytes());
        assertThat(Math.max(thumb.getWidth(), thumb.getHeight())).isEqualTo(400);
    }

    /**
     * The point of the whole class: a phone-sized photo must come out small enough to
     * load on mobile data. Uses photo-like content (smooth gradients plus texture)
     * because that is what a JPEG encoder actually sees in a saree photo.
     */
    @Test
    void bringsAPhoneSizedPhotoUnderTheWebBudget() throws IOException {
        byte[] photo = photoLikeJpeg(3024, 4032);

        ImageVariants variants = processor.process(jpegUpload(photo));

        assertThat(variants.web().bytes().length).isLessThan(250 * 1024);
        assertThat(variants.thumb().bytes().length).isLessThan(60 * 1024);
    }

    @Test
    void namesTheThumbnailAsTheWebFileWithTheThumbSuffix() throws IOException {
        ImageVariants variants = processor.process(jpegUpload(noisyJpeg(2000, 2000)));

        String web = variants.web().filename();
        assertThat(web).endsWith(".jpg");
        assertThat(variants.thumb().filename())
                .isEqualTo(web.replace(".jpg", ImageVariants.THUMB_SUFFIX + ".jpg"));
        // The delete path and the frontend both rebuild the thumbnail URL this way.
        assertThat(ImageVariants.thumbUrlFor("/uploads/" + web))
                .isEqualTo("/uploads/" + variants.thumb().filename());
    }

    @Test
    void neverEnlargesAnImageThatIsAlreadySmall() throws IOException {
        ImageVariants variants = processor.process(jpegUpload(noisyJpeg(300, 200)));

        BufferedImage web = decode(variants.web().bytes());
        assertThat(web.getWidth()).isEqualTo(300);
        assertThat(web.getHeight()).isEqualTo(200);
    }

    @Test
    void storesFormatsImageIoCannotDecodeUnchanged() {
        // A stub WEBP: the right content type, but bytes no ImageIO reader accepts.
        byte[] bytes = "RIFF____WEBPVP8 not-really-an-image".getBytes();
        MockMultipartFile upload = new MockMultipartFile("files", "s.webp", "image/webp", bytes);

        ImageVariants variants = processor.process(upload);

        assertThat(variants.web().filename()).endsWith(".webp");
        assertThat(variants.web().bytes()).isEqualTo(bytes);
        assertThat(variants.thumb().bytes()).isEqualTo(bytes);
        assertThat(variants.web().contentType()).isEqualTo("image/webp");
    }

    @Test
    void rejectsANonImageContentType() {
        MockMultipartFile upload =
                new MockMultipartFile("files", "notes.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> processor.process(upload))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void rejectsAnEmptyUpload() {
        MockMultipartFile upload = new MockMultipartFile("files", "a.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> processor.process(upload))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /** A transparent PNG has to land on white, not the black an empty RGB canvas gives. */
    @Test
    void flattensTransparencyOntoWhite() throws IOException {
        BufferedImage source = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(source, "png", png); // fully transparent
        MockMultipartFile upload =
                new MockMultipartFile("files", "logo.png", "image/png", png.toByteArray());

        ImageVariants variants = processor.process(upload);

        Color corner = new Color(decode(variants.web().bytes()).getRGB(5, 5));
        assertThat(corner.getRed()).isGreaterThan(240);
        assertThat(corner.getGreen()).isGreaterThan(240);
        assertThat(corner.getBlue()).isGreaterThan(240);
    }

    private MockMultipartFile jpegUpload(byte[] bytes) {
        return new MockMultipartFile("files", "saree.jpg", "image/jpeg", bytes);
    }

    private BufferedImage decode(byte[] bytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    /** Smooth gradients plus a fine weave texture — compresses the way a real photo does. */
    private byte[] photoLikeJpeg(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int r = (int) (127 + 120 * Math.sin(x / 300.0) * Math.cos(y / 420.0));
                int g = (int) (110 + 100 * Math.sin((x + y) / 260.0));
                int b = (int) (140 + 90 * Math.cos(y / 180.0));
                int weave = ((x / 3 + y / 3) % 2) * 8;
                image.setRGB(x, y, (clamp(r + weave) << 16) | (clamp(g) << 8) | clamp(b));
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    /** Random-noise JPEG: incompressible, so size assertions reflect the resize, not luck. */
    private byte[] noisyJpeg(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(42);
        Graphics2D g = image.createGraphics();
        for (int x = 0; x < width; x += 4) {
            for (int y = 0; y < height; y += 4) {
                g.setColor(new Color(random.nextInt(0xFFFFFF)));
                g.fillRect(x, y, 4, 4);
            }
        }
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }
}
