package com.dms.integration.controller;

import com.dms.common.BizException;
import com.dms.common.R;
import com.dms.oms.entity.ReplenishOrder;
import com.dms.oms.mapper.ReplenishOrderMapper;
import com.dms.oms.service.ReplenishService;
import com.dms.parts.service.PartStockService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** IR 控制塔：备件缺货 / 补货单快照，按经销商生成缺货补货单。 */
@RestController
@RequestMapping("/api/open/ir")
public class OpenIrController {
    private final PartStockService stockService;
    private final ReplenishService replenishService;
    private final ReplenishOrderMapper replenishMapper;
    private final String apiKey;

    public OpenIrController(
            PartStockService stockService,
            ReplenishService replenishService,
            ReplenishOrderMapper replenishMapper,
            @Value("${dms.open.api-key:dms-open-key}") String apiKey) {
        this.stockService = stockService;
        this.replenishService = replenishService;
        this.replenishMapper = replenishMapper;
        this.apiKey = apiKey;
    }

    @GetMapping("/snapshots")
    public R<Map<String, Object>> snapshots(
            @RequestHeader(value = "X-Api-Key", required = false) String key) {
        checkKey(key);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> shortage : stockService.shortage()) {
            rows.add(row("SHORTAGE",
                    shortage.get("dealerCode") + "/" + shortage.get("partNo"),
                    "SHORT",
                    String.valueOf(shortage.get("partNo")),
                    decimal(shortage.get("available")),
                    null,
                    String.valueOf(shortage.get("dealerCode")),
                    String.valueOf(shortage.get("name"))));
        }
        for (ReplenishOrder order : replenishMapper.selectList(null)) {
            rows.add(row("REPLENISH", order.getReplenishNo(), order.getStatus(),
                    null, BigDecimal.ONE, null, order.getDealerCode(),
                    "补货单 " + order.getReplenishNo()));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("system", "DMS");
        data.put("snapshots", rows);
        return R.ok(data);
    }

    @PostMapping("/actions")
    public R<Object> actions(
            @RequestHeader(value = "X-Api-Key", required = false) String key,
            @RequestBody Map<String, Object> body) {
        checkKey(key);
        String type = String.valueOf(body.getOrDefault("type", ""));
        String targetKey = String.valueOf(body.getOrDefault("targetKey", ""));
        @SuppressWarnings("unchecked")
        Map<String, Object> params = body.get("params") instanceof Map
                ? (Map<String, Object>) body.get("params") : new LinkedHashMap<String, Object>();
        if ("DMS_REPLENISH_SHORTAGE".equals(type)) {
            String dealer = first(string(params.get("dealerCode")), dealerOf(targetKey), targetKey);
            ReplenishOrder created = replenishService.createFromShortage(dealer);
            if (created == null) {
                throw new BizException("经销商无缺货: " + dealer);
            }
            return R.ok(created);
        }
        throw new BizException("不支持的 IR 指令: " + type);
    }

    private void checkKey(String key) {
        if (apiKey == null || apiKey.trim().isEmpty() || !apiKey.equals(key)) {
            throw new BizException("无效的 API Key");
        }
    }

    private static String dealerOf(String targetKey) {
        if (targetKey == null) {
            return null;
        }
        int slash = targetKey.indexOf('/');
        return slash > 0 ? targetKey.substring(0, slash) : targetKey;
    }

    private static Map<String, Object> row(
            String dataType, String bizKey, String status, String sku,
            BigDecimal qty, BigDecimal amount, String plantCode, String title) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("dataType", dataType);
        row.put("bizKey", bizKey);
        row.put("status", status);
        row.put("sku", sku);
        row.put("qty", qty);
        row.put("amount", amount);
        row.put("plantCode", plantCode);
        row.put("title", title);
        return row;
    }

    private static String first(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty() && !"null".equals(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value));
    }
}
