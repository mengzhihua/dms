package com.dms.oms.controller;

import com.dms.common.BaseCrudController;
import com.dms.common.BizException;
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

    /** 补货单状态/库存只能经业务接口变更,禁用通用增删改 */
    @Override
    public R<ReplenishOrder> create(ReplenishOrder entity) {
        throw new BizException("请使用 /draft 创建补货单");
    }

    @Override
    public R<ReplenishOrder> update(Long id, ReplenishOrder entity) {
        throw new BizException("补货单不支持直接修改");
    }

    @Override
    public R<Void> delete(Long id) {
        throw new BizException("补货单不支持删除,请使用取消");
    }

    /** body: {dealerCode, items:[{partNo,qty}], remark} */
    @PostMapping("/draft")
    @SuppressWarnings("unchecked")
    public R<ReplenishOrder> draft(@RequestBody Map<String, Object> body) {
        return R.ok(
                service.create(
                        (String) body.get("dealerCode"),
                        (List<Map<String, Object>>) body.get("items"),
                        (String) body.get("source"),
                        (String) body.get("remark")));
    }

    @PostMapping("/from-shortage")
    public R<ReplenishOrder> fromShortage(@RequestParam String dealerCode) {
        return R.ok(service.createFromShortage(dealerCode));
    }

    @PostMapping("/{id}/push")
    public R<ReplenishOrder> push(@PathVariable Long id) {
        return R.ok(service.push(id));
    }

    @PostMapping("/{id}/sync")
    public R<ReplenishOrder> sync(@PathVariable Long id) {
        return R.ok(service.sync(id));
    }

    @PostMapping("/sync-all")
    public R<Integer> syncAll() {
        return R.ok(service.syncAll());
    }

    @PostMapping("/{id}/cancel")
    public R<ReplenishOrder> cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(service.cancel(id, body == null ? null : (String) body.get("reason")));
    }

    @GetMapping("/{id}/lines")
    public R<List<Map<String, Object>>> lines(@PathVariable Long id) {
        return R.ok(service.lines(service.get(id)));
    }

    /** 查询 OMS 中心仓可售库存,partNos 逗号分隔 */
    @GetMapping("/oms-inventory")
    public R<List<Map<String, Object>>> omsInventory(@RequestParam String partNos) {
        return R.ok(service.omsInventory(Arrays.asList(partNos.split(","))));
    }
}
