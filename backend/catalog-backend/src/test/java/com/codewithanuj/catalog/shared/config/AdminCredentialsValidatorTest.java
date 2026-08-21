package com.codewithanuj.catalog.shared.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A13: production must not start with the development admin credentials.
 *
 * <p>The rules are unit-tested directly, then one context test proves the check is
 * actually wired to startup — a validator nobody calls would pass every rule test and
 * still let a wide-open deployment boot.
 */
class AdminCredentialsValidatorTest {

    private static final String GOOD_PASSWORD = "a-genuinely-long-admin-password";

    @Test
    void acceptsARealPassword() {
        assertThatCode(() -> AdminCredentialsValidator.check("owner", GOOD_PASSWORD))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsABlankUsername(String username) {
        assertThatThrownBy(() -> AdminCredentialsValidator.check(username, GOOD_PASSWORD))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_USERNAME");
    }

    @Test
    void rejectsAnUnsetUsername() {
        assertThatThrownBy(() -> AdminCredentialsValidator.check(null, GOOD_PASSWORD))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_USERNAME");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsABlankPassword(String password) {
        assertThatThrownBy(() -> AdminCredentialsValidator.check("owner", password))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD");
    }

    @Test
    void rejectsAnUnsetPassword() {
        assertThatThrownBy(() -> AdminCredentialsValidator.check("owner", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD");
    }

    @Test
    void rejectsTheShippedDevelopmentPassword() {
        // The exact value in application.properties, and the reason this class exists.
        assertThatThrownBy(() -> AdminCredentialsValidator.check("admin", "admin123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("well-known default");
    }

    @ParameterizedTest
    @ValueSource(strings = {"password", "changeme", "letmein", "secret", "ADMIN123", "PassWord"})
    void rejectsWellKnownPasswordsWhateverTheirCasing(String password) {
        assertThatThrownBy(() -> AdminCredentialsValidator.check("owner", password))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("well-known default");
    }

    @Test
    void rejectsAShortPassword() {
        String tooShort = "x".repeat(AdminCredentialsValidator.MIN_PASSWORD_LENGTH - 1);

        assertThatThrownBy(() -> AdminCredentialsValidator.check("owner", tooShort))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("shorter than");
    }

    @Test
    void acceptsAPasswordExactlyAtTheMinimumLength() {
        String atLimit = "x".repeat(AdminCredentialsValidator.MIN_PASSWORD_LENGTH);

        assertThatCode(() -> AdminCredentialsValidator.check("owner", atLimit))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAPasswordEqualToTheUsername() {
        String same = "administrator-account";

        assertThatThrownBy(() -> AdminCredentialsValidator.check(same, same.toUpperCase()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("same as ADMIN_USERNAME");
    }

    @Test
    void productionStartupFailsWithTheDevelopmentDefaults() {
        MockEnvironment environment = environmentWithProfiles("prod");
        environment.setProperty("admin.username", "admin");
        environment.setProperty("admin.password", "admin123");

        assertThatThrownBy(() -> postProcess(environment))
                .as("a deployment that forgot ADMIN_PASSWORD must not boot")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD");
    }

    @Test
    void productionStartupSucceedsWithRealCredentials() {
        MockEnvironment environment = environmentWithProfiles("prod");
        environment.setProperty("admin.username", "owner");
        environment.setProperty("admin.password", GOOD_PASSWORD);

        assertThatCode(() -> postProcess(environment)).doesNotThrowAnyException();
    }

    @Test
    void theCheckDoesNotApplyOutsideProduction() {
        // Local development and the test suite keep admin/admin123 and must still run.
        MockEnvironment environment = environmentWithProfiles("default");
        environment.setProperty("admin.username", "admin");
        environment.setProperty("admin.password", "admin123");

        assertThatCode(() -> postProcess(environment)).doesNotThrowAnyException();
    }

    @Test
    void theCheckRunsAfterEveryOtherEnvironmentPostProcessor() {
        // It reads admin.* from application-prod.properties and the environment, so it
        // must not run before those property sources are in place.
        assertThat(new AdminCredentialsValidator().getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    /**
     * The processor is registered in META-INF/spring.factories rather than being a bean.
     * If that entry is lost the class still compiles and every rule test above still
     * passes, while production quietly stops being checked at all.
     */
    @Test
    void theProcessorIsRegisteredInSpringFactories() {
        List<String> registered = SpringFactoriesLoader
                .forDefaultResourceLocation(AdminCredentialsValidator.class.getClassLoader())
                // Boot registers processors of its own that cannot be built without a
                // DeferredLogFactory (CloudFoundryVcapEnvironmentPostProcessor, for one).
                // Skip those; ours has a no-arg constructor, so if it is missing from the
                // result it is genuinely missing.
                .load(EnvironmentPostProcessor.class, (type, implementationName, failure) -> { })
                .stream()
                .map(processor -> processor.getClass().getName())
                .toList();

        assertThat(registered).contains(AdminCredentialsValidator.class.getName());
    }

    private static MockEnvironment environmentWithProfiles(String... profiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profiles);
        return environment;
    }

    private static void postProcess(MockEnvironment environment) {
        new AdminCredentialsValidator().postProcessEnvironment(environment, new SpringApplication());
    }
}
