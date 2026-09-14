package com.dms.workshop.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dms.common.R;
import com.dms.workshop.entity.WorkOrder;
import com.dms.workshop.entity.WorkOrderLabor;
import com.dms.workshop.entity.WorkOrderPart;
import com.dms.workshop.mapper.WorkOrderMapper;
import com.dms.workshop.service.WorkOrderService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workshop/order")
@RequiredArgsConstructor
public class WorkOrderController {
    private final WorkOrderService service;
    private final WorkOrderMapper orderMapper;

    @GetMapping("/page")
    public R<Page<WorkOrder>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(required = false) Long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dealerCode,
            @RequestParam(required = false) String plateNo) {
        QueryWrapper<WorkOrder> q = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            q.and(w -> w.or().like("order_no", keyword).or().like("vin", keyword));
        }
        if (status != null && !status.isEmpty()) {
            q.eq("status", status);
        }
        if (dealerCode != null && !dealerCode.isEmpty()) {
            q.eq("dealer_code", dealerCode);
        }
        if (plateNo != null && !plateNo.isEmpty()) {
            q.like("plate_no", plateNo);
        }
        q.orderByDesc("id");
        return R.ok(orderMapper.selectPage(new Page<>(page != null ? page : current, size), q));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(service.detail(id));
    }

    @PostMapping
    public R<WorkOrder> create(@RequestBody WorkOrder o) {
        return R.ok(service.create(o));
    }

    @PostMapping("/{id}/labor")
    public R<WorkOrderLabor> addLabor(@PathVariable Long id, @RequestBody WorkOrderLabor line) {
        return R.ok(service.addLabor(id, line));
    }

    @DeleteMapping("/{id}/labor/{lineId}")
    public R<Void> removeLabor(@PathVariable Long id, @PathVariable Long lineId) {
        service.removeLabor(id, lineId);
        return R.ok();
    }

    @PostMapping("/{id}/part")
    public R<WorkOrderPart> addPart(@PathVariable Long id, @RequestBody WorkOrderPart line) {
        return R.ok(service.addPart(id, line));
    }

    @DeleteMapping("/{id}/part/{lineId}")
    public R<Void> removePart(@PathVariable Long id, @PathVariable Long lineId) {
        service.removePart(id, lineId);
        return R.ok();
    }

    @PostMapping("/{id}/apply-guide/{guideCode}")
    public R<WorkOrder> applyGuide(@PathVariable Long id, @PathVariable String guideCode) {
        return R.ok(service.applyGuide(id, guideCode));
    }

    private String op(Map<String, Object> body) {
        return body != null && body.get("operator") != null
                ? body.get("operator").toString()
                : "system";
    }

    @PostMapping("/{id}/diagnose")
    public R<WorkOrder> diagnose(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(
                service.diagnose(
                        id, body != null ? (String) body.get("diagnosis") : null, op(body)));
    }

    @PostMapping("/{id}/quote")
    public R<WorkOrder> quote(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(service.quote(id, op(body)));
    }

    @PostMapping("/{id}/approve")
    public R<WorkOrder> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(service.approve(id, op(body)));
    }

    @PostMapping("/{id}/dispatch")
    public R<WorkOrder> dispatch(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(
                service.dispatch(
                        id,
                        (String) body.get("technicianCode"),
                        (String) body.get("bayCode"),
                        op(body)));
    }

    @PostMapping("/{id}/start")
    public R<WorkOrder> start(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(service.start(id, op(body)));
    }

    @PostMapping("/{id}/finish")
    public R<WorkOrder> finish(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(service.finish(id, op(body)));
    }

    @PostMapping("/{id}/qc")
    public R<WorkOrder> qc(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean pass = body.get("pass") == null || Boolean.TRUE.equals(body.get("pass"));
        return R.ok(service.qc(id, pass, (String) body.get("remark"), op(body)));
    }

    @PostMapping("/{id}/settle")
    public R<WorkOrder> settle(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(service.settle(id, body != null ? body : new java.util.HashMap<>(), op(body)));
    }

    @PostMapping("/{id}/deliver")
    public R<WorkOrder> deliver(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(service.deliver(id, op(body)));
    }

    @PostMapping("/{id}/close")
    public R<WorkOrder> close(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(service.close(id, op(body)));
    }

    @PostMapping("/{id}/cancel")
    public R<WorkOrder> cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(service.cancel(id, op(body)));
    }
}
