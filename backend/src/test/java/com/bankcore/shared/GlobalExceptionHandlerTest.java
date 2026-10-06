package com.bankcore.shared;

import com.bankcore.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void badRequest_returns400WithErrorCode() throws Exception {
        mockMvc.perform(get("/api/v1/test-errors/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.name()))
                .andExpect(jsonPath("$.message").value("Custom bad request message"))
                .andExpect(jsonPath("$.path").value("/api/v1/test-errors/bad-request"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void unauthorized_returns401WithErrorCode() throws Exception {
        mockMvc.perform(get("/api/v1/test-errors/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_CREDENTIALS.name()))
                .andExpect(jsonPath("$.path").value("/api/v1/test-errors/unauthorized"));
    }

    @Test
    void forbidden_returns403WithErrorCode() throws Exception {
        mockMvc.perform(get("/api/v1/test-errors/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.name()))
                .andExpect(jsonPath("$.path").value("/api/v1/test-errors/forbidden"));
    }

    @Test
    void notFound_returns404WithErrorCode() throws Exception {
        mockMvc.perform(get("/api/v1/test-errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(ErrorCode.ACCOUNT_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").value("Account 123 not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/test-errors/not-found"));
    }

    @Test
    void conflict_returns409WithErrorCode() throws Exception {
        mockMvc.perform(get("/api/v1/test-errors/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value(ErrorCode.INSUFFICIENT_BALANCE.name()))
                .andExpect(jsonPath("$.path").value("/api/v1/test-errors/conflict"));
    }

    @Test
    void unprocessable_returns422WithErrorCode() throws Exception {
        mockMvc.perform(get("/api/v1/test-errors/unprocessable"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(ErrorCode.ACCOUNT_CLOSE_REQUIRES_ZERO_BALANCE.name()))
                .andExpect(jsonPath("$.path").value("/api/v1/test-errors/unprocessable"));
    }

    @Test
    void runtimeException_returns500WithGenericError() throws Exception {
        mockMvc.perform(get("/api/v1/test-errors/runtime"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_ERROR.name()))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.path").value("/api/v1/test-errors/runtime"));
    }

    @Test
    void validationError_returns400WithDetails() throws Exception {
        String body = """
                {"name": "", "amount": 0}
                """;
        mockMvc.perform(post("/api/v1/test-errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.name()))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("name")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("amount")));
    }

    @Test
    void missingIdempotencyKey_returns400WithSpecificCode() throws Exception {
        mockMvc.perform(post("/api/v1/test-errors/idempotency")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(ErrorCode.MISSING_IDEMPOTENCY_KEY.name()));
    }

    @Test
    void malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/test-errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.name()))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
    }

    @Test
    void notFoundEndpoint_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/test-errors/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND.name()));
    }
}