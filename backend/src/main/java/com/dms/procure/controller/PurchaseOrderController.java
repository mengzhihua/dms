package com.dms.procure.controller;

import com.dms.common.BaseCrudController;
import com.dms.common.BizException;
import com.dms.common.R;
import com.dms.procure.entity.PurchaseOrder;
import com.dms.procure.entity.PurchaseOrderLine;
import com.dms.procure.mapper.PurchaseOrderMapper;
import com.dms.procure.service.PurchaseOrderService;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/procure/order")
public class PurchaseOrderController
        extends BaseCrudController<PurchaseOrder, PurchaseOrderMapper> {
    private final PurchaseOrderService service;

    public PurchaseOrderController(PurchaseOrderService service) {
        super(PurchaseOrder.class);
        this.service = service;
    }

    protected String[] keywordColumns() {
        return new String[] {"po_no", "oem_order_no"};
    }

    @Override
    @PostMapping
    public R<PurchaseOrder> create(@Valid @RequestBody PurchaseOrder entity) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("dealerCode", entity.getDealerCode());
        if (entity.getExpectDate() != null) {
            body.put("expectDate", entity.getExpectDate().toString());
        }
        body.put("remark", entity.getRemark());
        body.put("lines", entity.getLines());
        return R.ok(service.create(body));
    }

    @Override
    @PutMapping("/{id}")
    public R<PurchaseOrder> update(
            @PathVariable Long id, @Valid @RequestBody PurchaseOrder entity) {
        throw new BizException("采购订单请使用提交/确认/收货等专用接口修改");
    }

    @Override
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @PostMapping("/from-shortage")
    public R<PurchaseOrder> fromShortage(@RequestBody(required = false) Map<String, Object> body) {
        return R.ok(service.createFromShortage(
                body == null ? null : (String) body.get("dealerCode")));
    }

    @GetMapping("/{id}/lines")
    public R<List<PurchaseOrderLine>> lines(@PathVariable Long id) {
        return R.ok(service.lines(id));
    }

    @GetMapping("/{id}/receipts")
    public R<List<Map<String, Object>>> receipts(@PathVariable Long id) {
        return R.ok(service.receipts(id));
    }

    @PostMapping("/{id}/submit")
    public R<PurchaseOrder> submit(@PathVariable Long id) {
        return R.ok(service.submit(id));
    }

    @PostMapping("/{id}/confirm")
    public R<PurchaseOrder> confirm(
            @PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(service.confirm(id, body));
    }

    @PostMapping("/{id}/reject")
    public R<PurchaseOrder> reject(
            @PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(service.reject(id, body));
    }

    @PostMapping("/{id}/cancel")
    public R<PurchaseOrder> cancel(
            @PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(service.cancel(id, body));
    }

    @PostMapping("/{id}/receive")
    public R<PurchaseOrder> receive(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(service.receive(id, body));
    }

    @PostMapping("/{id}/close")
    public R<PurchaseOrder> close(@PathVariable Long id) {
        return R.ok(service.close(id));
    }
}
