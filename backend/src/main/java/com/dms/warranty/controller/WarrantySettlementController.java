package com.dms.warranty.controller;

import com.dms.common.BaseCrudController;
import com.dms.common.BizException;
import com.dms.common.R;
import com.dms.warranty.entity.WarrantyClaim;
import com.dms.warranty.entity.WarrantySettlement;
import com.dms.warranty.mapper.WarrantySettlementMapper;
import com.dms.warranty.service.WarrantySettlementService;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
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

    @Override
    @PostMapping
    public R<WarrantySettlement> create(@Valid @RequestBody WarrantySettlement entity) {
        throw new BizException("结算单只能通过归集生成，不可手工创建/修改");
    }

    @Override
    @PutMapping("/{id}")
    public R<WarrantySettlement> update(
            @PathVariable Long id, @Valid @RequestBody WarrantySettlement entity) {
        throw new BizException("结算单只能通过归集生成，不可手工创建/修改");
    }

    @Override
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
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
