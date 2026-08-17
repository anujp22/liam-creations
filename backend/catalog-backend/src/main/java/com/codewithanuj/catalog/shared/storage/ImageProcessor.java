package com.codewithanuj.catalog.shared.storage;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.filters.ImageFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * Validates an uploaded image and turns it into the two files we actually store: a
 * web-sized version for the product page and a thumbnail for listings.
 *
 * <p>The owner uploads 3-8MB photos straight off a phone. Serving those unchanged makes
 * a product page 20-40MB, which is unusable on mobile data — so every upload is
 * re-encoded down to roughly {@value #WEB_TARGET_BYTES} bytes before it is stored.
 *
 * <p>Formats ImageIO cannot decode (WEBP and animated GIF have no reader in a stock
 * JDK 17) are stored byte-for-byte instead of being rejected, with the thumbnail a copy
 * of the original. Those are already-compressed web formats, so the size problem this
 * class exists to solve does not really apply to them.
 */
@Component
public class ImageProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImageProcessor.class);

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_BYTES = 8L * 1024 * 1024; // 8 MB, matches multipart limit

    /**
     * Refuse absurdly large images before decoding them. Decoding is what costs memory
     * (~4 bytes per pixel), and this is checked against the file header, so a 200MP
     * image is rejected without ever being expanded into the heap.
     */
    private static final long MAX_PIXELS = 60_000_000L; // 60MP — well above any phone camera

    private static final int WEB_MAX_EDGE = 1600;
    private static final int THUMB_MAX_EDGE = 400;
    private static final int WEB_TARGET_BYTES = 220 * 1024;
    private static final float THUMB_QUALITY = 0.75f;

    /** Tried in order until the web image fits WEB_TARGET_BYTES; the last one is accepted regardless. */
    private static final float[] QUALITY_LADDER = {0.85f, 0.75f, 0.65f, 0.55f};

    /**
     * JPEG has no alpha channel. Without this a transparent PNG composites onto black;
     * Thumbnailator runs filters after the resize, so this works on the small image.
     */
    private static final ImageFilter FLATTEN_ONTO_WHITE = source -> {
        if (source.getTransparency() == Transparency.OPAQUE) {
            return source;
        }
        BufferedImage flat = new BufferedImage(
                source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = flat.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, flat.getWidth(), flat.getHeight());
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return flat;
    };

    /** Validates the upload and returns the web and thumbnail files to store. */
    public ImageVariants process(MultipartFile file) {
        byte[] original = validateAndRead(file);
        String id = UUID.randomUUID().toString();

        int[] size = readDimensions(original);
        if (size == null) {
            // No ImageIO reader for this format — store it as uploaded.
            String extension = extensionFor(file.getContentType());
            log.debug("No decoder for {}, storing {} bytes unresized", file.getContentType(), original.length);
            return new ImageVariants(
                    new ImageVariants.ImageFile(id + extension, original, file.getContentType()),
                    new ImageVariants.ImageFile(
                            id + ImageVariants.THUMB_SUFFIX + extension, original, file.getContentType()));
        }

        int longestEdge = Math.max(size[0], size[1]);
        try {
            byte[] web = encodeWithinBudget(original, longestEdge);
            byte[] thumb = encodeJpeg(original, boxFor(longestEdge, THUMB_MAX_EDGE), THUMB_QUALITY);
            log.debug("Resized upload {} -> web {} bytes, thumb {} bytes",
                    original.length, web.length, thumb.length);
            return new ImageVariants(
                    new ImageVariants.ImageFile(id + ".jpg", web, "image/jpeg"),
                    new ImageVariants.ImageFile(id + ImageVariants.THUMB_SUFFIX + ".jpg", thumb, "image/jpeg"));
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process image", e);
        }
    }

    private byte[] validateAndRead(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds 8 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only JPEG, PNG, WEBP or GIF images are allowed");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read upload", e);
        }
    }

    /**
     * Reads width and height from the file header without decoding the pixels, and
     * rejects anything over MAX_PIXELS. Returns null when no reader supports the format.
     */
    private int[] readDimensions(byte[] bytes) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (in == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if ((long) width * height > MAX_PIXELS) {
                    throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                            "Image resolution is too large to process");
                }
                return new int[]{width, height};
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            return null; // unreadable header — fall back to storing the original
        }
    }

    /** Steps down JPEG quality until the encoded image fits the byte budget. */
    private byte[] encodeWithinBudget(byte[] original, int longestEdge) throws IOException {
        int box = boxFor(longestEdge, WEB_MAX_EDGE);
        byte[] encoded = null;
        for (float quality : QUALITY_LADDER) {
            encoded = encodeJpeg(original, box, quality);
            if (encoded.length <= WEB_TARGET_BYTES) {
                return encoded;
            }
        }
        return encoded; // best effort — still far smaller than the original
    }

    /**
     * Thumbnailator reads the original bytes rather than a pre-decoded image so that it
     * applies the EXIF orientation tag; phone photos are routinely stored sideways with
     * the rotation only recorded in metadata.
     */
    private byte[] encodeJpeg(byte[] original, int box, float quality) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(original))
                .size(box, box)          // fits within the box, preserving aspect ratio
                .addFilter(FLATTEN_ONTO_WHITE)
                .outputFormat("jpg")
                .outputQuality(quality)
                .toOutputStream(out);
        return out.toByteArray();
    }

    /** Never enlarge: a photo already smaller than the target keeps its own dimensions. */
    private int boxFor(int longestEdge, int maxEdge) {
        return Math.min(longestEdge, maxEdge);
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}
