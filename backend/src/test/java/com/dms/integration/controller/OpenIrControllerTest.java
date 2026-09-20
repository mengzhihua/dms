package com.dms.integration.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:dms_open_ir;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "dms.oms.mock=true",
        "dms.open.api-key=dms-open-key"
})
@AutoConfigureMockMvc
public class OpenIrControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void shortageQtyIsGapNotAvailable() throws Exception {
        String snapshots = mockMvc.perform(get("/api/open/ir/snapshots")
                        .header("X-Api-Key", "dms-open-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.system").value("DMS"))
                .andReturn().getResponse().getContentAsString();
        JsonNode shortRow = null;
        for (JsonNode row : objectMapper.readTree(snapshots).get("data").get("snapshots")) {
            if ("SHORTAGE".equals(row.path("dataType").asText())
                    && row.path("bizKey").asText().contains("P-IR-SHORT")) {
                shortRow = row;
                break;
            }
        }
        assertNotNull(shortRow, "应包含 IR 缺货演示件");
        assertTrue(shortRow.path("qty").asInt() >= 9, "缺货数量应为 minStock-available");
    }
}
