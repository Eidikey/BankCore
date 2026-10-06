package com.bankcore.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests requiring a real PostgreSQL.
 *
 * <p>A single container ({@code postgres:16-alpine}, same major as
 * {@code docker-compose.yml}) is shared by all subclasses in the same JVM fork.
 * Liquibase is re-enabled here because {@code src/test/resources/application.yaml}
 * disables it for slice/unit tests (H2 cannot run the PostgreSQL changelogs).
 *
 * <p>Requires Docker. These tests use the {@code *IT} suffix so they run with
 * {@code ./mvnw verify} (failsafe), not with {@code ./mvnw test} (surefire).
 */
@Testcontainers
@SpringBootTest(properties = "spring.liquibase.enabled=true")
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");
}
