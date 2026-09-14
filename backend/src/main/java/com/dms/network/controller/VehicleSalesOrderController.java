package com.dms.network.controller;

import com.dms.common.BaseCrudController;
import com.dms.common.R;
import com.dms.network.entity.VehicleSalesOrder;
import com.dms.network.mapper.VehicleSalesOrderMapper;
import com.dms.network.service.NetworkService;
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
    public R<VehicleSalesOrder> create(@RequestBody VehicleSalesOrder entity) {
        return R.ok(service.createSalesOrder(entity));
    }

    @PostMapping("/{id}/allocate")
    public R<VehicleSalesOrder> allocate(@PathVariable Long id) {
        return R.ok(service.allocate(id));
    }

    @PostMapping("/{id}/invoice")
    public R<VehicleSalesOrder> invoice(@PathVariable Long id) {
        return R.ok(service.invoiceSalesOrder(id));
    }

    @PostMapping("/{id}/deliver")
    public R<VehicleSalesOrder> deliver(@PathVariable Long id) {
        return R.ok(service.deliver(id));
    }

    @PostMapping("/{id}/cancel")
    public R<VehicleSalesOrder> cancel(@PathVariable Long id) {
        return R.ok(service.cancelSalesOrder(id));
    }
}
