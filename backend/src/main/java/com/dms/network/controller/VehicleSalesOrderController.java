package com.dms.network.controller;

import com.dms.auth.DataScope;
import com.dms.common.BaseCrudController;
import com.dms.common.BizException;
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

    /** 仅新建状态可修改描述性字段；状态/金额/审批字段不允许直改。 */
    @Override
    public R<VehicleSalesOrder> update(
            @PathVariable Long id, @Valid @RequestBody VehicleSalesOrder entity) {
        VehicleSalesOrder cur = mapper.selectById(id);
        if (cur == null) {
            throw new BizException("销售订单不存在");
        }
        DataScope.check(cur.getDealerCode());
        if (!"NEW".equals(cur.getStatus())) {
            throw new BizException("订单当前状态不允许修改");
        }
        java.math.BigDecimal newDeposit =
                entity.getDeposit() == null ? java.math.BigDecimal.ZERO : entity.getDeposit();
        java.math.BigDecimal curDeposit =
                cur.getDeposit() == null ? java.math.BigDecimal.ZERO : cur.getDeposit();
        if (newDeposit.compareTo(curDeposit) != 0) {
            throw new BizException("定金已入账，不可修改，请通过收款登记调整");
        }
        if (entity.getPrice() == null
                || entity.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BizException("车价须大于 0");
        }
        if (cur.getLoanAmount() != null
                && cur.getLoanAmount().signum() > 0
                && ("APPLIED".equals(cur.getLoanStatus())
                        || "APPROVED".equals(cur.getLoanStatus()))
                && entity.getPrice().compareTo(cur.getLoanAmount()) < 0) {
            throw new BizException("车价不能低于已申请贷款金额");
        }
        cur.setCustomerId(entity.getCustomerId());
        cur.setModelCode(entity.getModelCode());
        cur.setVin(entity.getVin());
        cur.setColor(entity.getColor());
        cur.setPrice(entity.getPrice());
        cur.setRemark(entity.getRemark());
        mapper.updateById(cur);
        return R.ok(cur);
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
