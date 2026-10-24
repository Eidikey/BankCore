package com.bankcore.auth;

import com.bankcore.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthFlowIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "una-clave-segura";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void accessTokenHasFifteenMinuteLifetimeAndSessionClaims() throws Exception {
        String email = uniqueEmail("claims");
        register(email);
        SessionTokens tokens = login(email, PASSWORD);

        Jwt jwt = jwtDecoder.decode(tokens.accessToken());

        assertThat(jwt.getClaimAsString("iss")).isEqualTo("bankcore");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("CLIENT");
        assertThat(jwt.getClaimAsString("sid")).isNotBlank();
        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(jwt.getExpiresAt()).isNotNull();
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()))
                .isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void registerPersistsPersonalDataAndArgon2Hash() throws Exception {
        String email = uniqueEmail("ana");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, LocalDate.now().minusYears(30))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.firstName").value("Ana"))
                .andExpect(jsonPath("$.lastName").value("García"))
                .andExpect(jsonPath("$.age").value(30))
                .andExpect(jsonPath("$.gender").value("FEMALE"))
                .andExpect(jsonPath("$.occupation").value("Software engineer"))
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "select password_hash, first_name, last_name, birth_date, gender, occupation "
                        + "from users where email = ?", email);
        assertThat((String) row.get("password_hash")).startsWith("$argon2");
        assertThat(row.get("first_name")).isEqualTo("Ana");
        assertThat(row.get("last_name")).isEqualTo("García");
        assertThat(row.get("gender")).isEqualTo("FEMALE");
        assertThat(row.get("occupation")).isEqualTo("Software engineer");
    }

    @Test
    void loginCreatesSessionAndProtectedRouteAcceptsToken() throws Exception {
        String email = uniqueEmail("login");
        register(email);

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        String accessToken = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
        String refreshToken = JsonPath.read(login.getResponse().getContentAsString(), "$.refreshToken");

        Map<String, Object> session = jdbcTemplate.queryForMap(
                "select refresh_token_hash, expires_at from sessions "
                        + "where user_id = (select id from users where email = ?)", email);
        String storedHash = (String) session.get("refresh_token_hash");
        assertThat(storedHash).hasSize(64).isNotEqualTo(refreshToken);

        mockMvc.perform(get("/api/v1/test-errors/bad-request"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/test-errors/bad-request")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void expiredSessionCannotBeRefreshed() throws Exception {
        String email = uniqueEmail("expired");
        register(email);
        SessionTokens tokens = login(email, PASSWORD);

        jdbcTemplate.update(
                "update sessions set created_at = now() - interval '2 minutes', "
                        + "expires_at = now() - interval '1 minute' "
                        + "where user_id = (select id from users where email = ?)",
                email);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(tokens.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void disabledUserCannotRefreshOrUseExistingAccessToken() throws Exception {
        String email = uniqueEmail("disabled");
        register(email);
        SessionTokens tokens = login(email, PASSWORD);

        jdbcTemplate.update("update users set status = 'DISABLED' where email = ?", email);

        mockMvc.perform(get("/api/v1/test-errors/bad-request")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(tokens.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void duplicateEmailIsRejectedCaseInsensitively() throws Exception {
        String email = uniqueEmail("duplicate");
        register(email);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email.toUpperCase(), LocalDate.now().minusYears(25))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void threeFailedAttemptsLockAccountAndExpiredLockAllowsLogin() throws Exception {
        String email = uniqueEmail("locked");
        register(email);

        for (int attempt = 1; attempt <= 3; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody(email, "wrong-password")))
                    .andExpect(status().isUnauthorized());
        }

        Map<String, Object> locked = jdbcTemplate.queryForMap(
                "select failed_login_attempts, locked_until from users where email = ?", email);
        assertThat(((Number) locked.get("failed_login_attempts")).intValue()).isEqualTo(3);
        assertThat(locked.get("locked_until")).isNotNull();
        Object lockDeadline = locked.get("locked_until");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER_TEMPORARILY_LOCKED"));

        Map<String, Object> stillLocked = jdbcTemplate.queryForMap(
                "select failed_login_attempts, locked_until from users where email = ?", email);
        assertThat(((Number) stillLocked.get("failed_login_attempts")).intValue()).isEqualTo(3);
        assertThat(stillLocked.get("locked_until")).isEqualTo(lockDeadline);

        jdbcTemplate.update("update users set locked_until = now() - interval '1 minute' where email = ?", email);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, PASSWORD)))
                .andExpect(status().isOk());

        Map<String, Object> reset = jdbcTemplate.queryForMap(
                "select failed_login_attempts, locked_until from users where email = ?", email);
        assertThat(((Number) reset.get("failed_login_attempts")).intValue()).isZero();
        assertThat(reset.get("locked_until")).isNull();
    }

    @Test
    void logoutRevokesOnlyOneSession() throws Exception {
        String email = uniqueEmail("sessions");
        register(email);

        SessionTokens first = login(email, PASSWORD);
        SessionTokens second = login(email, PASSWORD);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(first.refreshToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(first.refreshToken())))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(second.refreshToken())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/test-errors/bad-request")
                        .header("Authorization", "Bearer " + first.accessToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void successfulLoginWritesAuditEvent() throws Exception {
        String email = uniqueEmail("audit-login");
        register(email);

        login(email, PASSWORD);

        Map<String, Object> event = jdbcTemplate.queryForMap(
                "select actor_user_id, target_type, target_id, ip_address, user_agent "
                        + "from audit_events where event_type = 'AUTH_LOGIN_SUCCESS' "
                        + "and actor_user_id = ?",
                userId(email));
        assertThat(event.get("actor_user_id")).isEqualTo(userId(email));
        assertThat(event.get("target_type")).isEqualTo("USER");
        assertThat(event.get("target_id")).isEqualTo(userId(email));
        assertThat(event.get("ip_address")).isNotNull();
        assertThat(event.get("user_agent")).isEqualTo("JUnit-Agent");
    }

    @Test
    void failedLoginsAndTemporaryLockWriteAuditEvents() throws Exception {
        String email = uniqueEmail("audit-lock");
        register(email);

        for (int attempt = 1; attempt <= 3; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .header("User-Agent", "JUnit-Agent")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody(email, "wrong-password")))
                    .andExpect(status().isUnauthorized());
        }

        Map<String, Object> counts = jdbcTemplate.queryForMap(
                "select count(*) filter (where event_type = 'AUTH_LOGIN_FAILURE') as failures, "
                        + "count(*) filter (where event_type = 'AUTH_TEMP_LOCK') as locks "
                        + "from audit_events where actor_user_id = ?",
                userId(email));
        assertThat(((Number) counts.get("failures")).intValue()).isEqualTo(3);
        assertThat(((Number) counts.get("locks")).intValue()).isEqualTo(1);

        Map<String, Object> lockEvent = jdbcTemplate.queryForMap(
                "select target_type, target_id, metadata::text as metadata "
                        + "from audit_events where event_type = 'AUTH_TEMP_LOCK' "
                        + "and actor_user_id = ?",
                userId(email));
        assertThat(lockEvent.get("target_type")).isEqualTo("USER");
        assertThat(lockEvent.get("target_id")).isEqualTo(userId(email));
        assertThat((String) lockEvent.get("metadata")).contains("lockedUntil");
    }

    @Test
    void logoutWritesAuditEvent() throws Exception {
        String email = uniqueEmail("audit-logout");
        register(email);
        SessionTokens tokens = login(email, PASSWORD);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("User-Agent", "JUnit-Agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(tokens.refreshToken())))
                .andExpect(status().isNoContent());

        Map<String, Object> event = jdbcTemplate.queryForMap(
                "select actor_user_id, target_type, target_id, user_agent "
                        + "from audit_events where event_type = 'AUTH_LOGOUT' "
                        + "and actor_user_id = ?",
                userId(email));
        assertThat(event.get("actor_user_id")).isEqualTo(userId(email));
        assertThat(event.get("target_type")).isEqualTo("USER");
        assertThat(event.get("target_id")).isEqualTo(userId(email));
        assertThat(event.get("user_agent")).isEqualTo("JUnit-Agent");
    }

    @Test
    void underageRegistrationIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(uniqueEmail("minor"), LocalDate.now().minusYears(17))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_AGE"));
    }

    @Test
    void invalidAccessTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/test-errors/bad-request")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void invalidRefreshTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"00000000-0000-0000-0000-000000000000"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, LocalDate.now().minusYears(30))))
                .andExpect(status().isCreated());
    }

    private SessionTokens login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .header("User-Agent", "JUnit-Agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return new SessionTokens(
                JsonPath.read(body, "$.accessToken"),
                JsonPath.read(body, "$.refreshToken"));
    }

    private record SessionTokens(String accessToken, String refreshToken) {
    }

    private String registerBody(String email, LocalDate birthDate) {
        return """
                {
                  "email":"%s",
                  "password":"%s",
                  "firstName":"Ana",
                  "lastName":"García",
                  "birthDate":"%s",
                  "gender":"FEMALE",
                  "occupation":"Software engineer"
                }
                """.formatted(email, PASSWORD, birthDate);
    }

    private String loginBody(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    private UUID userId(String email) {
        return jdbcTemplate.queryForObject(
                "select id from users where email = ?", UUID.class, email);
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}
