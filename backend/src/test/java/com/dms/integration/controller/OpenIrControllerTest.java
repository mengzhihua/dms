package com.dms.integration.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        JsonNode draft = null;
        for (JsonNode row : objectMapper.readTree(snapshots).get("data").get("snapshots")) {
            if ("SHORTAGE".equals(row.path("dataType").asText())
                    && row.path("bizKey").asText().contains("P-IR-SHORT")) {
                shortRow = row;
            }
            if ("RPL-IR-DRAFT".equals(row.path("bizKey").asText())) {
                draft = row;
            }
        }
        assertNotNull(shortRow, "应包含 IR 缺货演示件");
        assertTrue(shortRow.path("qty").asInt() >= 9, "缺货数量应为 minStock-available");
        assertNotNull(draft, "应包含 IR 草稿补货单");
        assertEquals("DRAFT", draft.path("status").asText());

        String dealer = shortRow.path("plantCode").asText();
        String created = mockMvc.perform(post("/api/open/ir/actions")
                        .header("X-Api-Key", "dms-open-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"DMS_REPLENISH_SHORTAGE\",\"targetKey\":\"" + dealer
                                + "\",\"idempotencyKey\":\"DMS-RPL-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        String replay = mockMvc.perform(post("/api/open/ir/actions")
                        .header("X-Api-Key", "dms-open-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"DMS_REPLENISH_SHORTAGE\",\"targetKey\":\"" + dealer
                                + "\",\"idempotencyKey\":\"DMS-RPL-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        String replenishNo = objectMapper.readTree(created).get("data").get("replenishNo").asText();
        assertEquals(replenishNo, objectMapper.readTree(replay).get("data").get("replenishNo").asText());

        mockMvc.perform(post("/api/open/ir/actions")
                        .header("X-Api-Key", "dms-open-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"DMS_PUSH_REPLENISH\",\"targetKey\":\"" + replenishNo
                                + "\",\"idempotencyKey\":\"DMS-PUSH-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PUSHED"));
        mockMvc.perform(post("/api/open/ir/actions")
                        .header("X-Api-Key", "dms-open-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"DMS_PUSH_REPLENISH\",\"targetKey\":\"" + replenishNo
                                + "\",\"idempotencyKey\":\"DMS-PUSH-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PUSHED"));

        mockMvc.perform(post("/api/open/ir/replenish-shortage")
                        .header("X-Api-Key", "dms-open-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dealerCode\":\"" + dealer + "\",\"idempotencyKey\":\"DMS-RPL-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.replenishNo").value(replenishNo));
        mockMvc.perform(post("/api/open/ir/push-replenish")
                        .header("X-Api-Key", "dms-open-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"replenishNo\":\"" + replenishNo
                                + "\",\"idempotencyKey\":\"DMS-PUSH-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PUSHED"));
    }
}
