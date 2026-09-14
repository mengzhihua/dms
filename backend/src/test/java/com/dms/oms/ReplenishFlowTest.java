package com.dms.oms;

import static org.junit.jupiter.api.Assertions.*;

import com.dms.common.BizException;
import com.dms.oms.entity.ReplenishOrder;
import com.dms.oms.service.ReplenishService;
import com.dms.parts.service.PartStockService;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 备件补货闭环(Mock OMS):草稿 -> 下单 -> 发货 -> 签收入库,且入库只发生一次;回推事件同样驱动状态机。 */
@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:dms_rpl_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "dms.oms.mock=true"
        })
class ReplenishFlowTest {
    @Autowired ReplenishService service;
    @Autowired PartStockService stockService;

    private static Map<String, Object> item(String partNo, int qty) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("partNo", partNo);
        m.put("qty", qty);
        return m;
    }

    @Test
    void pollDrivenFlowReceivesStockOnce() {
        int before = stockService.available("D001", "P0001");
        ReplenishOrder d = service.create("D001", Arrays.asList(item("P0001", 10), item("P0002", 3)), null, null);
        assertEquals(ReplenishService.DRAFT, d.getStatus());
        assertTrue(d.getReplenishNo().startsWith("RPL"));

        ReplenishOrder o = service.push(d.getId());
        assertEquals(ReplenishService.PUSHED, o.getStatus());
        assertNotNull(o.getOmsOrderNo());
        assertThrows(BizException.class, () -> service.push(o.getId()));

        ReplenishOrder s1 = service.sync(o.getId());
        assertEquals(ReplenishService.SHIPPED, s1.getStatus());
        assertNotNull(s1.getTrackingNo());
        assertEquals(before, stockService.available("D001", "P0001"));

        ReplenishOrder s2 = service.sync(o.getId());
        assertEquals(ReplenishService.RECEIVED, s2.getStatus());
        assertEquals("COMPLETED", s2.getOmsStatus());
        assertEquals(before + 10, stockService.available("D001", "P0001"));

        // 已入库后再同步/再回推都不会重复入库
        service.sync(o.getId());
        Map<String, Object> evt = new LinkedHashMap<>();
        evt.put("channelOrderNo", o.getReplenishNo());
        evt.put("status", "COMPLETED");
        service.onOmsEvent(evt);
        assertEquals(before + 10, stockService.available("D001", "P0001"));
        assertThrows(BizException.class, () -> service.cancel(o.getId(), "x"));
    }

    @Test
    void callbackDrivenFlowUsesShippedQtyAndCancel() {
        int before = stockService.available("D002", "P0003");
        ReplenishOrder o = service.push(
                service.create("D002", Collections.singletonList(item("P0003", 8)), null, null).getId());

        Map<String, Object> shipped = new LinkedHashMap<>();
        shipped.put("channelOrderNo", o.getReplenishNo());
        shipped.put("orderNo", o.getOmsOrderNo());
        shipped.put("status", "SHIPPED");
        shipped.put("trackingNo", "SF123");
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("sku", "P0003");
        line.put("qty", 8);
        line.put("shippedQty", 6);
        shipped.put("items", Collections.singletonList(line));
        assertEquals(ReplenishService.SHIPPED, service.onOmsEvent(shipped).getStatus());

        Map<String, Object> signed = new LinkedHashMap<>(shipped);
        signed.put("status", "COMPLETED");
        ReplenishOrder r = service.onOmsEvent(signed);
        assertEquals(ReplenishService.RECEIVED, r.getStatus());
        assertEquals(before + 6, stockService.available("D002", "P0003"));

        Map<String, Object> unknown = new LinkedHashMap<>();
        unknown.put("channelOrderNo", "RPL-NOPE");
        unknown.put("status", "SHIPPED");
        assertNull(service.onOmsEvent(unknown));

        ReplenishOrder c = service.push(
                service.create("D002", Collections.singletonList(item("P0003", 1)), null, null).getId());
        c = service.cancel(c.getId(), "不需要了");
        assertEquals(ReplenishService.CANCELLED, c.getStatus());
        assertEquals("CANCELLED", c.getOmsStatus());
    }

    @Test
    void shortageGeneratesDraftAndOmsInventoryQuery() {
        List<Map<String, Object>> inv = service.omsInventory(Arrays.asList("P0001", "P0002"));
        assertEquals(2, inv.size());
        assertThrows(BizException.class, () -> service.create("D001", Collections.emptyList(), null, null));
        assertThrows(BizException.class, () -> service.create("NOPE", Collections.singletonList(item("P0001", 1)), null, null));
        assertThrows(BizException.class, () -> service.create("D001", Collections.singletonList(item("P9999", 1)), null, null));
    }
}
