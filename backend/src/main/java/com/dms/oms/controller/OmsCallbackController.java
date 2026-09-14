package com.dms.oms.controller;

import com.dms.common.BizException;
import com.dms.common.R;
import com.dms.oms.entity.ReplenishOrder;
import com.dms.oms.service.ReplenishService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/** OMS -> DMS 状态回推入口;dms.oms.callback-key 非空时要求 X-Api-Key 一致。 */
@RestController
@RequestMapping("/api/open/oms")
public class OmsCallbackController {
    private final ReplenishService service;
    private final String callbackKey;

    public OmsCallbackController(
            ReplenishService service, @Value("${dms.oms.callback-key:}") String callbackKey) {
        this.service = service;
        this.callbackKey = callbackKey;
    }

    /** body: {event, orderNo, shopCode, channelOrderNo, status, warehouseCode, carrierCode, trackingNo, items:[{sku,qty,shippedQty}]} */
    @PostMapping("/orders/status")
    public R<ReplenishOrder> status(
            @RequestHeader(value = "X-Api-Key", required = false) String key,
            @RequestBody Map<String, Object> payload) {
        if (callbackKey != null && !callbackKey.isEmpty() && !callbackKey.equals(key)) {
            throw new BizException("X-Api-Key 无效");
        }
        ReplenishOrder o = service.onOmsEvent(payload);
        if (o == null) {
            throw new BizException("未知补货单: " + payload.get("channelOrderNo"));
        }
        return R.ok(o);
    }
}
