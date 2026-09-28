package com.dms.warranty.controller;

import com.dms.common.BaseCrudController;
import com.dms.common.R;
import com.dms.warranty.entity.WarrantyClaim;
import com.dms.warranty.entity.WarrantySettlement;
import com.dms.warranty.mapper.WarrantySettlementMapper;
import com.dms.warranty.service.WarrantySettlementService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/warranty/settlement")
public class WarrantySettlementController
        extends BaseCrudController<WarrantySettlement, WarrantySettlementMapper> {
    private final WarrantySettlementService service;

    public WarrantySettlementController(WarrantySettlementService service) {
        super(WarrantySettlement.class);
        this.service = service;
    }

    protected String[] keywordColumns() {
        return new String[] {"settlement_no"};
    }

    @PostMapping("/generate")
    public R<WarrantySettlement> generate(@RequestBody Map<String, Object> body) {
        return R.ok(service.generate(body));
    }

    @PostMapping("/{id}/confirm")
    public R<WarrantySettlement> confirm(@PathVariable Long id) {
        return R.ok(service.confirm(id));
    }

    @PostMapping("/{id}/pay")
    public R<WarrantySettlement> pay(@PathVariable Long id) {
        return R.ok(service.pay(id));
    }

    @GetMapping("/{id}/claims")
    public R<List<WarrantyClaim>> claims(@PathVariable Long id) {
        return R.ok(service.claims(id));
    }
}
