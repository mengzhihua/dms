package com.dms.customer.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.auth.DataScope;
import com.dms.common.BaseCrudController;
import com.dms.common.R;
import com.dms.customer.entity.Vehicle;
import com.dms.customer.entity.VehicleModel;
import com.dms.customer.mapper.VehicleMapper;
import com.dms.customer.mapper.VehicleModelMapper;
import com.dms.workshop.entity.WorkOrder;
import com.dms.workshop.mapper.WorkOrderMapper;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/vehicle")
public class VehicleController extends BaseCrudController<Vehicle, VehicleMapper> {
    private final WorkOrderMapper workOrderMapper;
    private final VehicleModelMapper modelMapper;
    private final VehicleMapper vehicleMapper;

    public VehicleController(WorkOrderMapper w, VehicleModelMapper m, VehicleMapper v) {
        super(Vehicle.class);
        this.workOrderMapper = w;
        this.modelMapper = m;
        this.vehicleMapper = v;
    }

    protected String[] keywordColumns() {
        return new String[] {"vin", "plate_no"};
    }

    @GetMapping("/vin/{vin}/history")
    public R<List<WorkOrder>> history(@PathVariable String vin) {
        Vehicle veh =
                vehicleMapper.selectOne(
                        new QueryWrapper<Vehicle>().eq("vin", vin).last("LIMIT 1"));
        if (veh != null) {
            DataScope.check(veh.getDealerCode());
        }
        return R.ok(
                workOrderMapper.selectList(
                        new QueryWrapper<WorkOrder>()
                                .eq("vin", vin)
                                .orderByDesc("id")));
    }

    /** 保修校验：保修期内（日期）且里程未超保修里程 → inWarranty。 */
    @GetMapping("/{id}/warranty-check")
    public R<Map<String, Object>> warrantyCheck(
            @PathVariable Long id, @RequestParam(required = false) Integer mileage) {
        Vehicle v = vehicleMapper.selectById(id);
        Map<String, Object> m = new LinkedHashMap<>();
        if (v != null) {
            DataScope.check(v.getDealerCode());
        }
        if (v == null) {
            m.put("inWarranty", false);
            m.put("reason", "车辆不存在");
            return R.ok(m);
        }
        boolean inDate = v.getWarrantyEnd() == null || !LocalDate.now().isAfter(v.getWarrantyEnd());
        Integer km = mileage != null ? mileage : v.getMileage();
        VehicleModel model =
                v.getModelCode() == null
                        ? null
                        : modelMapper.selectOne(
                                new QueryWrapper<VehicleModel>().eq("code", v.getModelCode()));
        Integer warrantyKm = model == null ? null : model.getWarrantyKm();
        boolean inKm = warrantyKm == null || km == null || km <= warrantyKm;
        boolean ok = inDate && inKm;
        m.put("inWarranty", ok);
        m.put(
                "reason",
                ok
                        ? "在保修期内"
                        : (!inDate ? "已过保修期(" + v.getWarrantyEnd() + ")" : "里程超出保修里程"));
        m.put("warrantyEnd", v.getWarrantyEnd());
        m.put("warrantyKm", warrantyKm);
        return R.ok(m);
    }
}
