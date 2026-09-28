package com.dms.procure.controller;

import com.dms.common.BaseCrudController;
import com.dms.common.BizException;
import com.dms.common.R;
import com.dms.procure.entity.PurchaseOrder;
import com.dms.procure.entity.PurchaseStatement;
import com.dms.procure.mapper.PurchaseStatementMapper;
import com.dms.procure.service.PurchaseStatementService;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/procure/statement")
public class PurchaseStatementController
        extends BaseCrudController<PurchaseStatement, PurchaseStatementMapper> {
    private final PurchaseStatementService service;

    public PurchaseStatementController(PurchaseStatementService service) {
        super(PurchaseStatement.class);
        this.service = service;
    }

    protected String[] keywordColumns() {
        return new String[] {"statement_no"};
    }

    @Override
    @PostMapping
    public R<PurchaseStatement> create(@Valid @RequestBody PurchaseStatement entity) {
        throw new BizException("对账单只能通过归集生成，不可手工创建/修改");
    }

    @Override
    @PutMapping("/{id}")
    public R<PurchaseStatement> update(
            @PathVariable Long id, @Valid @RequestBody PurchaseStatement entity) {
        throw new BizException("对账单只能通过归集生成，不可手工创建/修改");
    }

    @Override
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @PostMapping("/generate")
    public R<PurchaseStatement> generate(@RequestBody Map<String, Object> body) {
        return R.ok(service.generate(body));
    }

    @PostMapping("/{id}/confirm")
    public R<PurchaseStatement> confirm(@PathVariable Long id) {
        return R.ok(service.confirm(id));
    }

    @PostMapping("/{id}/pay")
    public R<PurchaseStatement> pay(@PathVariable Long id) {
        return R.ok(service.pay(id));
    }

    @GetMapping("/{id}/orders")
    public R<List<PurchaseOrder>> orders(@PathVariable Long id) {
        return R.ok(service.orders(id));
    }
}
