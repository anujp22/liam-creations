package com.codewithanuj.catalog.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the production Flyway configuration: schema migrations come from
 * classpath:db/migration, while the 100 demo products (V2, V3) live in
 * classpath:db/seed and must never reach a real catalog.
 *
 * This runs the migration chain the way application-prod.properties configures it,
 * deliberately without the seed location.
 */
@DataJpaTest
@TestPropertySource(properties = "spring.flyway.locations=classpath:db/migration")
class FlywaySeedIsolationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void productionMigrationsSeedNoProducts() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class);

        assertThat(count)
                .as("a fresh production database must start with an empty catalog")
                .isZero();
    }

    @Test
    void instagramPostUrlColumnIsDroppedEvenWithoutTheSeedMigrations() {
        // V1 created this column as TEXT NOT NULL and V3 (a seed migration) dropped it.
        // Without V13 it would survive in prod and reject every insert, since the
        // Product entity never populates it.
        Integer columns = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE UPPER(table_name) = 'PRODUCTS' "
                        + "AND UPPER(column_name) = 'INSTAGRAM_POST_URL'",
                Integer.class);

        assertThat(columns)
                .as("V13 must drop instagram_post_url on databases that never ran V3")
                .isZero();
    }
}
