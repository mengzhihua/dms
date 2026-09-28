package com.dms.procure.controller;

import com.dms.common.BaseCrudController;
import com.dms.common.BizException;
import com.dms.common.R;
import com.dms.procure.entity.PurchaseInquiry;
import com.dms.procure.entity.PurchaseInquiryLine;
import com.dms.procure.entity.PurchaseOrder;
import com.dms.procure.mapper.PurchaseInquiryMapper;
import com.dms.procure.service.PurchaseInquiryService;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/procure/inquiry")
public class PurchaseInquiryController
        extends BaseCrudController<PurchaseInquiry, PurchaseInquiryMapper> {
    private final PurchaseInquiryService service;

    public PurchaseInquiryController(PurchaseInquiryService service) {
        super(PurchaseInquiry.class);
        this.service = service;
    }

    protected String[] keywordColumns() {
        return new String[] {"inquiry_no", "title"};
    }

    @Override
    @PostMapping
    public R<PurchaseInquiry> create(@Valid @RequestBody PurchaseInquiry entity) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("dealerCode", entity.getDealerCode());
        body.put("title", entity.getTitle());
        if (entity.getExpectDate() != null) {
            body.put("expectDate", entity.getExpectDate().toString());
        }
        body.put("remark", entity.getRemark());
        body.put("lines", entity.getLines());
        return R.ok(service.create(body));
    }

    @Override
    @PutMapping("/{id}")
    public R<PurchaseInquiry> update(
            @PathVariable Long id, @Valid @RequestBody PurchaseInquiry entity) {
        throw new BizException("询价单请使用明细/发出等专用接口修改");
    }

    @PutMapping("/{id}/lines")
    public R<PurchaseInquiry> replaceLines(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(service.replaceLines(id, body));
    }

    @Override
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @GetMapping("/{id}/lines")
    public R<List<PurchaseInquiryLine>> lines(@PathVariable Long id) {
        return R.ok(service.lines(id));
    }

    @PostMapping("/{id}/send")
    public R<PurchaseInquiry> send(@PathVariable Long id) {
        return R.ok(service.send(id));
    }

    @PostMapping("/{id}/quote")
    public R<PurchaseInquiry> quote(
            @PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(service.quote(id, body));
    }

    @PostMapping("/{id}/close")
    public R<PurchaseInquiry> close(@PathVariable Long id) {
        return R.ok(service.close(id));
    }

    @PostMapping("/{id}/order")
    public R<PurchaseOrder> toOrder(@PathVariable Long id) {
        return R.ok(service.toOrder(id));
    }
}
