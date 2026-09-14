package com.dms.parts.controller;

import com.dms.common.BaseCrudController;
import com.dms.common.R;
import com.dms.parts.entity.PartStock;
import com.dms.parts.mapper.PartStockMapper;
import com.dms.parts.service.PartStockService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parts/stock")
public class PartStockController extends BaseCrudController<PartStock, PartStockMapper> {
    private final PartStockService service;

    public PartStockController(PartStockService service) {
        super(PartStock.class);
        this.service = service;
    }

    protected String[] keywordColumns() {
        return new String[] {"part_no", "location"};
    }

    @PostMapping("/inbound")
    public R<PartStock> inbound(@RequestBody Map<String, Object> body) {
        return R.ok(
                service.inbound(
                        (String) body.get("dealerCode"),
                        (String) body.get("partNo"),
                        (String) body.get("location"),
                        (String) body.get("batchNo"),
                        ((Number) body.get("qty")).intValue()));
    }

    @GetMapping("/shortage")
    public R<List<Map<String, Object>>> shortage() {
        return R.ok(service.shortage());
    }

    @GetMapping("/available")
    public R<Integer> available(@RequestParam String dealerCode, @RequestParam String partNo) {
        return R.ok(service.available(dealerCode, partNo));
    }
}
