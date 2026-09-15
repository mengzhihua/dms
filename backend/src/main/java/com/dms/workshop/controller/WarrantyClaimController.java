package com.dms.workshop.controller;

import com.dms.auth.DataScope;
import com.dms.common.BaseCrudController;
import com.dms.common.BizException;
import com.dms.common.R;
import com.dms.workshop.entity.WarrantyClaim;
import com.dms.workshop.mapper.WarrantyClaimMapper;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workshop/claim")
public class WarrantyClaimController extends BaseCrudController<WarrantyClaim, WarrantyClaimMapper> {
    public WarrantyClaimController() {
        super(WarrantyClaim.class);
    }

    protected String[] keywordColumns() {
        return new String[] {"claim_no"};
    }

    private WarrantyClaim transit(Long id, String from, String to) {
        WarrantyClaim c = mapper.selectById(id);
        if (c == null) {
            throw new BizException("索赔单不存在");
        }
        DataScope.check(c.getDealerCode());
        if (from != null && !from.equals(c.getStatus())) {
            throw new BizException("非法状态流转: " + c.getStatus() + " -> " + to);
        }
        c.setStatus(to);
        mapper.updateById(c);
        return c;
    }

    @PostMapping("/{id}/approve")
    public R<WarrantyClaim> approve(@PathVariable Long id) {
        return R.ok(transit(id, "SUBMITTED", "APPROVED"));
    }

    @PostMapping("/{id}/reject")
    public R<WarrantyClaim> reject(@PathVariable Long id) {
        return R.ok(transit(id, "SUBMITTED", "REJECTED"));
    }

    @PostMapping("/{id}/pay")
    public R<WarrantyClaim> pay(@PathVariable Long id) {
        return R.ok(transit(id, "APPROVED", "PAID"));
    }
}
