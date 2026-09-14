package com.dms.oms.controller;

import com.dms.auth.DataScope;
import com.dms.common.BaseCrudController;
import com.dms.common.R;
import com.dms.oms.entity.ReplenishOrder;
import com.dms.oms.mapper.ReplenishOrderMapper;
import com.dms.oms.service.ReplenishService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/oms/replenish")
public class ReplenishController extends BaseCrudController<ReplenishOrder, ReplenishOrderMapper> {
    private final ReplenishService service;

    public ReplenishController(ReplenishService service) {
        super(ReplenishOrder.class);
        this.service = service;
    }

    protected String[] keywordColumns() {
        return new String[] {"replenish_no", "dealer_code", "oms_order_no", "tracking_no"};
    }

    /** body: {dealerCode, items:[{partNo,qty}], remark} */
    @PostMapping("/draft")
    @SuppressWarnings("unchecked")
    public R<ReplenishOrder> draft(@RequestBody Map<String, Object> body) {
        return R.ok(
                service.create(
                        DataScope.effectiveDealer((String) body.get("dealerCode")),
                        (List<Map<String, Object>>) body.get("items"),
                        (String) body.get("source"),
                        (String) body.get("remark")));
    }

    @PostMapping("/from-shortage")
    public R<ReplenishOrder> fromShortage(@RequestParam String dealerCode) {
        return R.ok(service.createFromShortage(DataScope.effectiveDealer(dealerCode)));
    }

    private ReplenishOrder scopedGet(Long id) {
        ReplenishOrder o = service.get(id);
        DataScope.check(o == null ? null : o.getDealerCode());
        return o;
    }

    @PostMapping("/{id}/push")
    public R<ReplenishOrder> push(@PathVariable Long id) {
        scopedGet(id);
        return R.ok(service.push(id));
    }

    @PostMapping("/{id}/sync")
    public R<ReplenishOrder> sync(@PathVariable Long id) {
        scopedGet(id);
        return R.ok(service.sync(id));
    }

    @PostMapping("/sync-all")
    public R<Integer> syncAll() {
        return R.ok(service.syncAll());
    }

    @PostMapping("/{id}/cancel")
    public R<ReplenishOrder> cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        scopedGet(id);
        return R.ok(service.cancel(id, body == null ? null : (String) body.get("reason")));
    }

    @GetMapping("/{id}/lines")
    public R<List<Map<String, Object>>> lines(@PathVariable Long id) {
        return R.ok(service.lines(scopedGet(id)));
    }

    /** 查询 OMS 中心仓可售库存,partNos 逗号分隔 */
    @GetMapping("/oms-inventory")
    public R<List<Map<String, Object>>> omsInventory(@RequestParam String partNos) {
        return R.ok(service.omsInventory(Arrays.asList(partNos.split(","))));
    }
}
