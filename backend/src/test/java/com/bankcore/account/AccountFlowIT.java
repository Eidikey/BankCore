package com.bankcore.account;

import com.bankcore.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AccountFlowIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "una-clave-segura";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsAccountWithTenDigitNumberActiveAndZeroBalance() throws Exception {
        String email = uniqueEmail("account-create");
        register(email);
        String token = login(email);

        Account account = createAccount(token);

        assertThat(account.number()).matches("^10\\d{8}$");

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "select account_number, account_type, status, balance, user_id "
                        + "from accounts where id = ?", account.id());
        assertThat(row.get("account_number")).isEqualTo(account.number());
        assertThat(row.get("account_type")).isEqualTo("CUENTA_CORRIENTE");
        assertThat(row.get("status")).isEqualTo("ACTIVA");
        assertThat((BigDecimal) row.get("balance")).isEqualByComparingTo("0.00");
        assertThat(row.get("user_id")).isEqualTo(userId(email));
    }

    @Test
    void listsOnlyOwnAccountsIncludingClosedOnes() throws Exception {
        String ownerEmail = uniqueEmail("account-owner");
        register(ownerEmail);
        String ownerToken = login(ownerEmail);

        Account first = createAccount(ownerToken);
        Account second = createAccount(ownerToken);

        String otherEmail = uniqueEmail("account-other");
        register(otherEmail);
        String otherToken = login(otherEmail);
        Account other = createAccount(otherToken);

        jdbcTemplate.update(
                "update accounts set status = 'CERRADA', closed_at = now(), balance = 0.00 where id = ?",
                first.id());

        MvcResult result = mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn();

        List<String> numbers = JsonPath.read(result.getResponse().getContentAsString(), "$[*].accountNumber");
        assertThat(numbers).containsExactlyInAnyOrder(first.number(), second.number());
        assertThat(numbers).doesNotContain(other.number());

        List<Map<String, Object>> accounts = JsonPath.read(result.getResponse().getContentAsString(), "$");
        String firstStatus = accounts.stream()
                .filter(account -> first.number().equals(account.get("accountNumber")))
                .findFirst()
                .orElseThrow()
                .get("status")
                .toString();
        assertThat(firstStatus).isEqualTo("CERRADA");

        mockMvc.perform(get("/api/v1/accounts/{id}", first.id())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CERRADA"));
    }

    @Test
    void getsOwnAccountButHidesAnotherCustomersAccount() throws Exception {
        String ownerEmail = uniqueEmail("account-get-owner");
        register(ownerEmail);
        String ownerToken = login(ownerEmail);
        Account own = createAccount(ownerToken);

        String otherEmail = uniqueEmail("account-get-other");
        register(otherEmail);
        String otherToken = login(otherEmail);
        Account other = createAccount(otherToken);

        mockMvc.perform(get("/api/v1/accounts/{id}", own.id())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(own.id().toString()))
                .andExpect(jsonPath("$.accountNumber").value(own.number()));

        mockMvc.perform(get("/api/v1/accounts/{id}", other.id())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void rejectsSixthOpenAccountAndAllowsItAfterClosingOne() throws Exception {
        String email = uniqueEmail("account-limit");
        register(email);
        String token = login(email);

        for (int i = 0; i < 5; i++) {
            createAccount(token);
        }

        mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LIMIT_EXCEEDED"));

        UUID closedId = jdbcTemplate.queryForObject(
                "select id from accounts where user_id = ? and status <> 'CERRADA' order by created_at limit 1",
                UUID.class, userId(email));
        jdbcTemplate.update(
                "update accounts set status = 'CERRADA', closed_at = now(), balance = 0.00 where id = ?",
                closedId);

        createAccount(token);
    }

    private Account createAccount(String accessToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andReturn();
        return Account.from(result);
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"%s",
                                  "password":"%s",
                                  "firstName":"Ana",
                                  "lastName":"García",
                                  "birthDate":"%s",
                                  "gender":"FEMALE",
                                  "occupation":"Software engineer"
                                }
                                """.formatted(email, PASSWORD, LocalDate.now().minusYears(30))))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private UUID userId(String email) {
        return jdbcTemplate.queryForObject(
                "select id from users where email = ?", UUID.class, email);
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    private record Account(UUID id, String number) {
        private static Account from(MvcResult result) throws Exception {
            String body = result.getResponse().getContentAsString();
            return new Account(
                    UUID.fromString(JsonPath.read(body, "$.id")),
                    JsonPath.read(body, "$.accountNumber"));
        }
    }
}
