package com.codewithanuj.catalog.seo;

import com.codewithanuj.catalog.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves a sitemap of the public storefront so search engines can discover every
 * page. URLs are built from {@code app.public-base-url} (the storefront domain),
 * which differs from the API host.
 */
@RestController
public class SitemapController {

    private static final String[] STATIC_PATHS = {"/", "/sale", "/built-on-request"};

    private final ProductRepository productRepository;
    private final String baseUrl;

    public SitemapController(ProductRepository productRepository,
                             @Value("${app.public-base-url:http://localhost:5173}") String baseUrl) {
        this.productRepository = productRepository;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        for (String path : STATIC_PATHS) {
            appendUrl(xml, path);
        }
        for (String productNumber : productRepository.findActiveProductNumbers()) {
            appendUrl(xml, "/products/" + productNumber);
        }

        xml.append("</urlset>\n");
        return xml.toString();
    }

    private void appendUrl(StringBuilder xml, String path) {
        xml.append("  <url><loc>").append(baseUrl).append(path).append("</loc></url>\n");
    }
}
