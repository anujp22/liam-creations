package com.codewithanuj.catalog.shared.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/**
 * Refuses to start the application in production unless the admin credentials have
 * actually been set to something.
 *
 * <p>The admin login is the only thing standing between the public internet and the
 * whole catalog — creating, editing and deleting every product. There is no second
 * factor and no account lockout. {@code application.properties} ships
 * {@code admin/admin123} so local development works out of the box, and without this
 * check a deployment that forgot {@code ADMIN_PASSWORD} would inherit that default and
 * come up looking perfectly healthy.
 *
 * <p>Failing to start is deliberate, and follows the same reasoning already applied to
 * {@code PUBLIC_BASE_URL}: a container that will not boot is a loud, immediate problem,
 * while a container that boots with a guessable admin password is a silent one.
 *
 * <p><strong>Why an {@link EnvironmentPostProcessor} and not a {@code @PostConstruct}
 * bean.</strong> A bean-based check works, but it runs somewhere in the middle of
 * context refresh with no ordering guarantee against Flyway. Tried that first: with the
 * database unreachable, the operator got a Flyway connection stack trace and no mention
 * of the password at all. This runs immediately after the environment is prepared,
 * before a single bean exists and before anything touches the database, so the message
 * about the real problem is the first and only thing printed.
 *
 * <p>Prod profile only. Development and the test suite keep their weak defaults.
 *
 * <p>Registered in {@code META-INF/spring.factories} — this class is never a bean.
 */
public class AdminCredentialsValidator implements EnvironmentPostProcessor, Ordered {

    /** Long enough that guessing is hopeless; this is typed by one person, rarely. */
    static final int MIN_PASSWORD_LENGTH = 12;

    /**
     * Passwords rejected outright. {@code admin123} is the shipped development default
     * and the one this check exists for; the rest are the obvious first guesses.
     */
    static final Set<String> FORBIDDEN_PASSWORDS = Set.of(
            "admin123", "admin", "password", "changeme", "letmein", "secret");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean isProduction = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (!isProduction) {
            return;
        }
        check(environment.getProperty("admin.username"), environment.getProperty("admin.password"));
    }

    /**
     * Run last, so the property sources from {@code application-prod.properties} and the
     * environment variables are all in place by the time we read them.
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * @throws IllegalStateException with a message naming the environment variable to
     *         fix. Spring surfaces this as a startup failure.
     */
    static void check(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalStateException(
                    "ADMIN_USERNAME is not set. Refusing to start: the admin login would fall back "
                            + "to the development default.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD is not set. Refusing to start: the admin login would fall back "
                            + "to the development default 'admin123', leaving the whole catalog open.");
        }
        if (FORBIDDEN_PASSWORDS.contains(password.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD is set to a well-known default. Refusing to start: choose a "
                            + "password that is not on the first page of every guessing list.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD is shorter than " + MIN_PASSWORD_LENGTH + " characters. Refusing "
                            + "to start: the admin login has no rate limit on failed attempts and no "
                            + "second factor, so length is the only real defence.");
        }
        if (password.equalsIgnoreCase(username)) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD is the same as ADMIN_USERNAME. Refusing to start.");
        }
    }
}
