package com.codewithanuj.catalog.shared.storage;

/**
 * The two files produced for every uploaded image: a web-sized version shown on the
 * product page and a thumbnail used in listings and admin tables.
 *
 * <p>The two are tied together by filename, not by a database column: a thumbnail is
 * always the web filename with {@value #THUMB_SUFFIX} inserted before the extension.
 * That keeps one URL per image in {@code product_images} (no migration, no second
 * column to keep in sync) at the cost of the convention being load-bearing — both
 * {@link StorageService#delete} and the frontend derive the thumbnail URL this way.
 */
public record ImageVariants(ImageFile web, ImageFile thumb) {

    public static final String THUMB_SUFFIX = "-thumb";

    /** One file ready to be written: its name, its bytes and the type to serve it as. */
    public record ImageFile(String filename, byte[] bytes, String contentType) {}

    /**
     * Derives the thumbnail URL for a stored image URL, or null if the URL has no
     * extension to insert before (in which case there is no thumbnail to look for).
     */
    public static String thumbUrlFor(String url) {
        if (url == null) {
            return null;
        }
        int dot = url.lastIndexOf('.');
        int slash = url.lastIndexOf('/');
        if (dot <= slash) {
            return null; // no extension in the last path segment
        }
        return url.substring(0, dot) + THUMB_SUFFIX + url.substring(dot);
    }
}
