package com.dms.crm.controller;

import com.dms.common.BaseCrudController;
import com.dms.common.BizException;
import com.dms.common.R;
import com.dms.crm.entity.NotifyMessage;
import com.dms.crm.mapper.NotifyMessageMapper;
import com.dms.crm.service.NotifyService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** 通知记录：只读 + FAILED 重发。 */
@RestController
@RequestMapping("/api/crm/message")
public class NotifyMessageController
        extends BaseCrudController<NotifyMessage, NotifyMessageMapper> {
    private final NotifyService service;

    public NotifyMessageController(NotifyService service) {
        super(NotifyMessage.class);
        this.service = service;
    }

    protected String[] keywordColumns() {
        return new String[] {"receiver", "template_code", "content", "provider_ref"};
    }

    @Override
    @PostMapping
    public R<NotifyMessage> create(@Valid @RequestBody NotifyMessage entity) {
        throw new BizException("通知消息由系统生成，不可手工创建");
    }

    @Override
    @PutMapping("/{id}")
    public R<NotifyMessage> update(
            @PathVariable Long id, @Valid @RequestBody NotifyMessage entity) {
        throw new BizException("通知消息不可修改");
    }

    @Override
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        throw new BizException("通知消息不可删除");
    }

    @PostMapping("/{id}/retry")
    public R<NotifyMessage> retry(@PathVariable Long id) {
        return R.ok(service.retry(id));
    }
}
