package com.codewithanuj.catalog.shared.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the split migration chain introduced for A14.
 *
 * <p>The indexes are partial ({@code CREATE INDEX ... WHERE deleted = false}) because
 * measurement showed plain indexes on the same columns were either unused or beaten.
 * H2, which the whole test suite runs on, rejects that syntax, so those migrations live
 * in {@code db/migration-postgresql} — a sibling of {@code db/migration} rather than a
 * subdirectory, because Flyway scans locations recursively.
 *
 * <p>Both halves of that arrangement fail <em>silently</em> if broken, which is why they
 * are asserted here rather than left to a comment:
 * <ul>
 *   <li>Drop the location from the prod config and production quietly goes back to
 *       sequential scans on every storefront listing. Nothing errors.</li>
 *   <li>Put a partial index in {@code db/migration} and every H2 test dies at once —
 *       loud, but the message points at Flyway rather than at the real mistake.</li>
 * </ul>
 */
class PostgresOnlyMigrationsTest {

    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final Path PORTABLE = RESOURCES.resolve("db/migration");
    private static final Path POSTGRES_ONLY = RESOURCES.resolve("db/migration-postgresql");
    private static final String POSTGRES_LOCATION = "classpath:db/migration-postgresql";

    @Test
    void theProductionConfigStillLoadsThePostgresOnlyMigrations() throws IOException {
        String prod = Files.readString(RESOURCES.resolve("application-prod.properties"));

        assertThat(flywayLocations(prod))
                .as("without this, production runs without the A14 indexes and nothing says so")
                .contains(POSTGRES_LOCATION);
    }

    @Test
    void theDevelopmentConfigLoadsThemToo() throws IOException {
        String dev = Files.readString(RESOURCES.resolve("application.properties"));

        assertThat(flywayLocations(dev))
                .as("local Postgres should match production's schema")
                .contains(POSTGRES_LOCATION);
    }

    @Test
    void thePostgresOnlyDirectoryIsNotInsideThePortableOne() {
        // Flyway scans recursively. A subdirectory would be handed to H2 as well.
        assertThat(POSTGRES_ONLY.startsWith(PORTABLE)).isFalse();
        assertThat(POSTGRES_ONLY).exists();
    }

    @Test
    void theH2MigrationChainContainsNoPartialIndexes() throws IOException {
        try (Stream<Path> files = Files.walk(PORTABLE)) {
            List<Path> offenders = files
                    .filter(path -> path.toString().endsWith(".sql"))
                    .filter(PostgresOnlyMigrationsTest::declaresAPartialIndex)
                    .toList();

            assertThat(offenders)
                    .as("H2 rejects CREATE INDEX ... WHERE; these belong in db/migration-postgresql")
                    .isEmpty();
        }
    }

    @Test
    void everyPostgresOnlyMigrationIsAnIndex() throws IOException {
        // This location is skipped entirely on H2, so anything here is untested by the
        // suite. Indexes are safe to leave unexercised — they change speed, not results.
        // A table or column change would not be, and must go in the portable chain.
        try (Stream<Path> files = Files.walk(POSTGRES_ONLY)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".sql")).toList()) {
                String statements = stripComments(Files.readString(file));

                assertThat(statements.toUpperCase())
                        .as("%s changes more than indexes, so it cannot skip the test database", file)
                        .doesNotContain("CREATE TABLE")
                        .doesNotContain("ALTER TABLE")
                        .doesNotContain("DROP TABLE")
                        .doesNotContain("INSERT INTO")
                        .doesNotContain("UPDATE ");
            }
        }
    }

    private static boolean declaresAPartialIndex(Path file) {
        try {
            String sql = stripComments(Files.readString(file)).toUpperCase();
            int index = sql.indexOf("CREATE INDEX");
            while (index >= 0) {
                int end = sql.indexOf(';', index);
                String statement = end < 0 ? sql.substring(index) : sql.substring(index, end);
                if (statement.contains("WHERE")) return true;
                index = sql.indexOf("CREATE INDEX", index + 1);
            }
            return false;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + file, e);
        }
    }

    /** Comments explain the partial indexes, so matching on them would be self-defeating. */
    private static String stripComments(String sql) {
        return sql.lines()
                .filter(line -> !line.stripLeading().startsWith("--"))
                .reduce("", (a, b) -> a + "\n" + b);
    }

    private static String flywayLocations(String properties) {
        return properties.lines()
                .filter(line -> line.startsWith("spring.flyway.locations="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no spring.flyway.locations set"));
    }
}
