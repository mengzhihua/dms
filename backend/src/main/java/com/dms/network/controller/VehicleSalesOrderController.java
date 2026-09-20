package com.dms.network.controller;

import com.dms.common.BaseCrudController;
import com.dms.common.R;
import com.dms.network.entity.SalesPayment;
import com.dms.network.entity.VehicleSalesOrder;
import com.dms.network.mapper.VehicleSalesOrderMapper;
import com.dms.network.service.NetworkService;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/network/sales-order")
public class VehicleSalesOrderController
        extends BaseCrudController<VehicleSalesOrder, VehicleSalesOrderMapper> {
    private final NetworkService service;

    public VehicleSalesOrderController(NetworkService service) {
        super(VehicleSalesOrder.class);
        this.service = service;
    }

    protected String[] keywordColumns() {
        return new String[] {"order_no", "vin", "model_code"};
    }

    @Override
    public R<VehicleSalesOrder> create(@Valid @RequestBody VehicleSalesOrder entity) {
        return R.ok(service.createSalesOrder(entity));
    }

    @PostMapping("/{id}/allocate")
    public R<VehicleSalesOrder> allocate(@PathVariable Long id) {
        return R.ok(service.allocate(id));
    }

    @PostMapping("/{id}/finance")
    public R<VehicleSalesOrder> finance(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(service.applyFinance(id, body));
    }

    @PostMapping("/{id}/finance/decision")
    public R<VehicleSalesOrder> financeDecision(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(service.financeDecision(id, body));
    }

    @PostMapping("/{id}/insurance")
    public R<VehicleSalesOrder> insurance(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(service.insurance(id, body));
    }

    @PostMapping("/{id}/payment")
    public R<VehicleSalesOrder> payment(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(service.addPayment(id, body));
    }

    @GetMapping("/{id}/payments")
    public R<List<SalesPayment>> payments(@PathVariable Long id) {
        return R.ok(service.payments(id));
    }

    @GetMapping("/{id}/detail")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(service.salesDetail(id));
    }

    @PostMapping("/{id}/invoice")
    public R<VehicleSalesOrder> invoice(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(service.invoiceSalesOrder(id, body));
    }

    @PostMapping("/{id}/deliver")
    public R<VehicleSalesOrder> deliver(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(service.deliver(id, body));
    }

    @PostMapping("/{id}/cancel")
    public R<VehicleSalesOrder> cancel(@PathVariable Long id) {
        return R.ok(service.cancelSalesOrder(id));
    }
}
