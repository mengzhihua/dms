package com.dms.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Open IR：缺失/错误 API Key 返回 HTTP 401；请求体非法 JSON 返回 HTTP 400。 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:dms_open_ir_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "dms.open.api-key=ir-test-key"
})
@AutoConfigureMockMvc
class OpenIrApiTest {
    @Autowired MockMvc mvc;

    @Test
    void missingOrWrongKeyReturns401() throws Exception {
        mvc.perform(get("/api/open/ir/snapshots"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("无效的 API Key"));
        mvc.perform(get("/api/open/ir/snapshots").header("X-Api-Key", "bad"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/open/ir/snapshots").header("X-Api-Key", "ir-test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.system").value("DMS"));
    }

    @Test
    void malformedJsonBodyReturns400() throws Exception {
        mvc.perform(post("/api/open/ir/actions")
                        .header("X-Api-Key", "ir-test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("请求体格式错误"));
    }
}
