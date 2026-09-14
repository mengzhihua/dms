package com.dms.oms.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 本地/演示用 OMS 模拟:内存保存订单,每次查询状态向前推进一步
 * PUSHED -> SHIPPED -> COMPLETED,便于不启动 OMS 也能跑通补货闭环。
 */
public class MockOmsClient implements OmsClient {
    private final Map<String, Map<String, Object>> orders = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong();

    private static String key(Object shop, Object no) {
        return shop + "|" + no;
    }

    @Override
    public Map<String, Object> createOrder(Map<String, Object> request) {
        String k = key(request.get("shopCode"), request.get("channelOrderNo"));
        return orders.computeIfAbsent(
                k,
                x -> {
                    Map<String, Object> o = new LinkedHashMap<>();
                    o.put("orderNo", "SO-MOCK-" + seq.incrementAndGet());
                    o.put("shopCode", request.get("shopCode"));
                    o.put("channelOrderNo", request.get("channelOrderNo"));
                    o.put("status", "PUSHED");
                    o.put("warehouseCode", "WH-SH");
                    o.put("items", request.get("items"));
                    return o;
                });
    }

    @Override
    public Map<String, Object> getOrder(String shopCode, String channelOrderNo) {
        Map<String, Object> o = orders.get(key(shopCode, channelOrderNo));
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o.get("status"));
        if ("PUSHED".equals(s)) {
            o.put("status", "SHIPPED");
            o.put("carrierCode", "SF");
            o.put("trackingNo", "SF-MOCK-" + o.get("orderNo"));
        } else if ("SHIPPED".equals(s)) {
            o.put("status", "COMPLETED");
        }
        return new LinkedHashMap<>(o);
    }

    @Override
    public Map<String, Object> cancelOrder(String shopCode, String channelOrderNo, String reason) {
        Map<String, Object> o = orders.get(key(shopCode, channelOrderNo));
        if (o == null) {
            throw new OmsException("订单不存在");
        }
        if ("SHIPPED".equals(o.get("status")) || "COMPLETED".equals(o.get("status"))) {
            throw new OmsException("订单已发货,不能取消");
        }
        o.put("status", "CANCELLED");
        o.put("cancelReason", reason);
        return new LinkedHashMap<>(o);
    }

    @Override
    public List<Map<String, Object>> inventory(String shopCode, List<String> skus) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (String sku : skus) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sku", sku);
            m.put("qty", 200);
            out.add(m);
        }
        return out;
    }
}
