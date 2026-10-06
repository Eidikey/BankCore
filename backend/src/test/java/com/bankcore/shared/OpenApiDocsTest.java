package com.bankcore.shared;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the OpenAPI base configuration (TECHNICAL-DESIGN #41):
 * the JSON contract is served and carries BankCore metadata,
 * the JWT scheme and the versioned /api/v1 paths.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class OpenApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsAreServedWithBankCoreMetadata() throws Exception {
        mockMvc.perform(get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").value("3.1.0"))
                .andExpect(jsonPath("$.info.title").value("BankCore API"))
                .andExpect(jsonPath("$.info.version").value("1.0"));
    }

    @Test
    void apiDocsExposeJwtSchemeAndVersionedPaths() throws Exception {
        mockMvc.perform(get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.paths./api/v1/test-errors/bad-request.get").exists());
    }

    @Test
    void apiDocsDescribeStandardErrorSchema() throws Exception {
        mockMvc.perform(get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.type").value("object"))
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.properties.timestamp").exists())
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.properties.code").exists())
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.properties.message").exists())
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.properties.status").exists())
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.properties.path").exists());
    }
}
