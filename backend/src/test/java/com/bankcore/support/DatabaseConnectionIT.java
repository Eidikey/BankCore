package com.bankcore.support;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for the Testcontainers base infrastructure.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>the PostgreSQL container starts and Spring connects to it,</li>
 *   <li>Liquibase applied the master changelog (all 7 domain tables exist).</li>
 * </ul>
 *
 * <p>Run with {@code ./mvnw verify} (requires Docker).
 */
class DatabaseConnectionIT extends AbstractIntegrationTest {

    private static final List<String> EXPECTED_TABLES = List.of(
            "users",
            "sessions",
            "accounts",
            "transfers",
            "movements",
            "idempotency_keys",
            "audit_events"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void connectionIsRealPostgreSQL() {
        String version = jdbcTemplate.queryForObject("SELECT version()", String.class);

        assertThat(version).contains("PostgreSQL");
    }

    @Test
    void liquibaseCreatedAllDomainTables() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename",
                String.class);

        assertThat(tables).containsAll(EXPECTED_TABLES);
    }
}
