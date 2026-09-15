package com.dms.oms;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.dms.common.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dms.oms.client.OmsClient;
import com.dms.oms.client.OmsException;
import com.dms.oms.entity.ReplenishOrder;
import com.dms.oms.mapper.ReplenishOrderMapper;
import com.dms.oms.service.ReplenishService;
import com.dms.parts.service.PartStockService;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** OMS 调用失败时错误必须落库且单据回到可重试状态;未配置回推 key 时回推入口拒绝请求;通用增删改被禁用。 */
@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:dms_rpl_fail_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "dms.oms.callback-key="
        })
@AutoConfigureMockMvc
class ReplenishFailureTest {
    @Autowired ReplenishService service;
    @Autowired MockMvc mvc;
    @Autowired PartStockService stockService;
    @Autowired ReplenishOrderMapper mapper;
    @MockBean OmsClient oms;

    private static Map<String, Object> item(String partNo, int qty) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("partNo", partNo);
        m.put("qty", qty);
        return m;
    }

    @Test
    void pushFailurePersistsErrorAndRevertsToDraft() {
        when(oms.createOrder(any())).thenThrow(new OmsException("OMS 超时"));
        ReplenishOrder d = service.create("D001", Collections.singletonList(item("P0001", 1)), null, null);
        assertThrows(BizException.class, () -> service.push(d.getId()));
        ReplenishOrder after = service.get(d.getId());
        assertEquals(ReplenishService.DRAFT, after.getStatus());
        assertNotNull(after.getLastError());
        assertTrue(after.getLastError().contains("OMS 超时"));
        assertNull(after.getPushedAt());
    }

    @Test
    void syncFailurePersistsError() {
        Map<String, Object> created = new LinkedHashMap<>();
        created.put("orderNo", "SO-1");
        created.put("status", "CREATED");
        when(oms.createOrder(any())).thenReturn(created);
        when(oms.getOrder(anyString(), anyString())).thenThrow(new OmsException("连接被拒绝"));
        ReplenishOrder o = service.push(
                service.create("D001", Collections.singletonList(item("P0002", 1)), null, null).getId());
        assertEquals(ReplenishService.PUSHED, o.getStatus());
        assertEquals("SO-1", o.getOmsOrderNo());
        assertThrows(BizException.class, () -> service.sync(o.getId()));
        assertTrue(service.get(o.getId()).getLastError().contains("连接被拒绝"));
    }

    @Test
    void mergedQtyOverflowRejected() {
        assertThrows(BizException.class, () -> service.create("D001",
                Arrays.asList(item("P0003", 2_000_000_000), item("P0003", 2_000_000_000)), null, null));
    }

    @Test
    void cancelRejectedWhileOmsOrderIsBeingCreated() {
        ReplenishOrder d = service.create("D001", Collections.singletonList(item("P0004", 1)), null, null);
        when(oms.createOrder(any())).thenAnswer(inv -> {
            BizException ex = assertThrows(BizException.class, () -> service.cancel(d.getId(), "并发取消"));
            assertTrue(ex.getMessage().contains("正在下单"));
            Map<String, Object> created = new LinkedHashMap<>();
            created.put("orderNo", "SO-2");
            created.put("status", "CREATED");
            return created;
        });
        ReplenishOrder o = service.push(d.getId());
        assertEquals(ReplenishService.PUSHED, o.getStatus());
        assertEquals("SO-2", o.getOmsOrderNo());
    }

    @Test
    void staleSnapshotDoesNotOverwriteNewerMetadata() {
        Map<String, Object> created = new LinkedHashMap<>();
        created.put("orderNo", "SO-3");
        created.put("status", "CREATED");
        when(oms.createOrder(any())).thenReturn(created);
        ReplenishOrder o = service.push(
                service.create("D001", Collections.singletonList(item("P0006", 1)), null, null).getId());

        Map<String, Object> shipped = new LinkedHashMap<>();
        shipped.put("status", "SHIPPED");
        shipped.put("trackingNo", "OLD");
        Map<String, Object> completed = new LinkedHashMap<>();
        completed.put("status", "COMPLETED");
        completed.put("trackingNo", "NEW");
        // 轮询已读取本地单(PUSHED),在等待 OMS 返回旧 SHIPPED 快照期间,COMPLETED 回推先到达并完成入库
        when(oms.getOrder(anyString(), anyString())).thenAnswer(inv -> {
            Map<String, Object> evt = new LinkedHashMap<>(completed);
            evt.put("channelOrderNo", o.getReplenishNo());
            assertEquals(ReplenishService.RECEIVED, service.onOmsEvent(evt).getStatus());
            return shipped;
        });
        int before = stockService.available("D001", "P0006");
        ReplenishOrder r = service.sync(o.getId());
        assertEquals(ReplenishService.RECEIVED, r.getStatus());
        assertEquals("COMPLETED", r.getOmsStatus());
        assertEquals("NEW", r.getTrackingNo());
        assertEquals(before + 1, stockService.available("D001", "P0006"));
    }

    @Test
    void completedStillReceivesWhenAnotherThreadAlreadyMovedToShipped() {
        Map<String, Object> created = new LinkedHashMap<>();
        created.put("orderNo", "SO-4");
        created.put("status", "CREATED");
        when(oms.createOrder(any())).thenReturn(created);
        ReplenishOrder o = service.push(
                service.create("D001", Collections.singletonList(item("P0007", 2)), null, null).getId());

        Map<String, Object> completed = new LinkedHashMap<>();
        completed.put("orderNo", "SO-4");
        completed.put("status", "COMPLETED");
        // 轮询线程已读到 PUSHED,等待 OMS 返回 COMPLETED 期间,SHIPPED 回推先完成 PUSHED->SHIPPED
        when(oms.getOrder(anyString(), anyString())).thenAnswer(inv -> {
            Map<String, Object> evt = new LinkedHashMap<>();
            evt.put("channelOrderNo", o.getReplenishNo());
            evt.put("status", "SHIPPED");
            assertEquals(ReplenishService.SHIPPED, service.onOmsEvent(evt).getStatus());
            return completed;
        });
        int before = stockService.available("D001", "P0007");
        ReplenishOrder r = service.sync(o.getId());
        assertEquals(ReplenishService.RECEIVED, r.getStatus());
        assertEquals(before + 2, stockService.available("D001", "P0007"));
        // 重复 COMPLETED 不再入库
        assertEquals(ReplenishService.RECEIVED, service.sync(o.getId()).getStatus());
        assertEquals(before + 2, stockService.available("D001", "P0007"));
    }

    @Test
    void interruptedPushingRecoveredFromOms() {
        ReplenishOrder exists = service.create("D001", Collections.singletonList(item("P0008", 1)), null, null);
        ReplenishOrder missing = service.create("D001", Collections.singletonList(item("P0009", 1)), null, null);
        // 模拟进程在 createOrder 前后中断:本地停在 PUSHING
        assertEquals(1, mapper.transit(exists.getId(), ReplenishService.DRAFT, ReplenishService.PUSHING));
        assertEquals(1, mapper.transit(missing.getId(), ReplenishService.DRAFT, ReplenishService.PUSHING));

        Map<String, Object> remote = new LinkedHashMap<>();
        remote.put("orderNo", "SO-5");
        remote.put("status", "AUDITED");
        when(oms.getOrder(anyString(), eq(exists.getReplenishNo()))).thenReturn(remote);
        when(oms.getOrder(anyString(), eq(missing.getReplenishNo()))).thenReturn(null);

        // 刚进入 PUSHING 未超时:syncAll 不干预正在进行的下单
        service.syncAll();
        assertEquals(ReplenishService.PUSHING, service.get(exists.getId()).getStatus());
        assertThrows(BizException.class, () -> service.push(exists.getId()));
        assertThrows(BizException.class, () -> service.cancel(exists.getId(), "x"));

        // 启动恢复:OMS 已建单 -> PUSHED 并补写元数据;OMS 无单 -> 回退 DRAFT
        assertEquals(2, service.recoverPushing(false));
        ReplenishOrder e = service.get(exists.getId());
        assertEquals(ReplenishService.PUSHED, e.getStatus());
        assertEquals("SO-5", e.getOmsOrderNo());
        assertEquals("AUDITED", e.getOmsStatus());
        assertNotNull(e.getPushedAt());
        ReplenishOrder m = service.get(missing.getId());
        assertEquals(ReplenishService.DRAFT, m.getStatus());
        assertNotNull(m.getLastError());
        // 回退后可重新下单
        Map<String, Object> created = new LinkedHashMap<>();
        created.put("orderNo", "SO-6");
        created.put("status", "CREATED");
        when(oms.createOrder(any())).thenReturn(created);
        assertEquals(ReplenishService.PUSHED, service.push(missing.getId()).getStatus());
    }

    @Test
    void callbackRejectedWhenKeyNotConfigured() throws Exception {
        mvc.perform(post("/api/open/oms/orders/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channelOrderNo\":\"RPL-X\",\"status\":\"COMPLETED\"}"))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void genericWriteEndpointsDisabled() throws Exception {
        String login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString();
        String token = new ObjectMapper().readTree(login).path("data").path("token").asText();
        mvc.perform(post("/api/oms/replenish")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"replenishNo\":\"RPL-HACK\",\"dealerCode\":\"D001\",\"status\":\"SHIPPED\"}"))
                .andExpect(jsonPath("$.code").value(400));
    }
}
