package com.codewithanuj.catalog.shared.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/** Builds the S3 client, only when {@code app.storage=s3}. */
@Configuration
@ConditionalOnProperty(name = "app.storage", havingValue = "s3")
public class S3Config {

    /**
     * Credentials come from the default chain — environment variables locally, the task
     * or instance role once deployed. Never put access keys in a properties file.
     */
    @Bean
    S3Client s3Client(@Value("${app.storage.s3.region}") String region,
                      @Value("${app.storage.s3.endpoint:}") String endpoint) {
        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create());

        // Set only to talk to an S3-compatible service (MinIO, LocalStack) instead of
        // AWS; those need path-style addressing because they have no per-bucket DNS.
        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }
        return builder.build();
    }
}
