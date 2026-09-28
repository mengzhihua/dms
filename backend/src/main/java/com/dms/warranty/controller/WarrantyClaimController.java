package com.dms.warranty.controller;

import com.dms.common.BaseCrudController;
import com.dms.common.BizException;
import com.dms.common.R;
import com.dms.warranty.entity.WarrantyClaim;
import com.dms.warranty.entity.WarrantyClaimLine;
import com.dms.warranty.mapper.WarrantyClaimMapper;
import com.dms.warranty.service.WarrantyClaimService;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/warranty/claim")
public class WarrantyClaimController extends BaseCrudController<WarrantyClaim, WarrantyClaimMapper> {
    private final WarrantyClaimService service;

    public WarrantyClaimController(WarrantyClaimService service) {
        super(WarrantyClaim.class);
        this.service = service;
    }

    protected String[] keywordColumns() {
        return new String[] {"claim_no", "vin", "plate_no"};
    }

    @GetMapping("/{id}/lines")
    public R<List<WarrantyClaimLine>> lines(@PathVariable Long id) {
        return R.ok(service.lines(id));
    }

    @Override
    @PutMapping("/{id}")
    public R<WarrantyClaim> update(@PathVariable Long id, @RequestBody WarrantyClaim entity) {
        Map<String, Object> body = new java.util.HashMap<>();
        if (entity.getFaultCode() != null) {
            body.put("faultCode", entity.getFaultCode());
        }
        if (entity.getFaultDesc() != null) {
            body.put("faultDesc", entity.getFaultDesc());
        }
        if (entity.getRemark() != null) {
            body.put("remark", entity.getRemark());
        }
        return R.ok(service.update(id, body));
    }

    @Override
    @PostMapping
    public R<WarrantyClaim> create(@Valid @RequestBody WarrantyClaim entity) {
        throw new BizException("索赔单由工单结算自动生成，不可手工创建");
    }

    @Override
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @PostMapping("/{id}/submit")
    public R<WarrantyClaim> submit(@PathVariable Long id) {
        return R.ok(service.submit(id));
    }

    @PostMapping("/{id}/ship")
    public R<WarrantyClaim> ship(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(service.ship(id, body));
    }

    @PostMapping("/{id}/approve")
    public R<WarrantyClaim> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(service.approve(id, body));
    }

    @PostMapping("/{id}/reject")
    public R<WarrantyClaim> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(service.reject(id, body));
    }

    @PostMapping("/{id}/return")
    public R<WarrantyClaim> returnBack(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(service.returnBack(id, body));
    }

    @PostMapping("/{id}/receive")
    public R<WarrantyClaim> receive(@PathVariable Long id) {
        return R.ok(service.receive(id));
    }
}
