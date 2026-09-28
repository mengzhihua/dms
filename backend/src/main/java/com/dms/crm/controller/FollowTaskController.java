package com.dms.crm.controller;

import com.dms.common.BaseCrudController;
import com.dms.common.BizException;
import com.dms.common.R;
import com.dms.crm.entity.FollowTask;
import com.dms.crm.entity.NotifyMessage;
import com.dms.crm.mapper.FollowTaskMapper;
import com.dms.crm.service.FollowTaskGenerator;
import com.dms.crm.service.FollowTaskService;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** 客户跟进任务：手工创建走实体（经销商=登录人），其余动作走专用端点。 */
@RestController
@RequestMapping("/api/crm/task")
public class FollowTaskController extends BaseCrudController<FollowTask, FollowTaskMapper> {
    private final FollowTaskService service;
    private final FollowTaskGenerator generator;

    public FollowTaskController(FollowTaskService service, FollowTaskGenerator generator) {
        super(FollowTask.class);
        this.service = service;
        this.generator = generator;
    }

    protected String[] keywordColumns() {
        return new String[] {"task_no", "customer_name", "phone", "vin", "plate_no", "title"};
    }

    @Override
    @PostMapping
    public R<FollowTask> create(@Valid @RequestBody FollowTask entity) {
        return R.ok(service.create(entity));
    }

    @Override
    @PutMapping("/{id}")
    public R<FollowTask> update(@PathVariable Long id, @Valid @RequestBody FollowTask entity) {
        throw new BizException("跟进任务请通过完成/取消/改派端点更新");
    }

    @Override
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok(null);
    }

    @PostMapping("/{id}/complete")
    public R<FollowTask> complete(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(service.complete(id, body == null ? null : str(body.get("result"))));
    }

    @PostMapping("/{id}/cancel")
    public R<FollowTask> cancel(@PathVariable Long id) {
        return R.ok(service.cancel(id));
    }

    @PostMapping("/{id}/assign")
    public R<FollowTask> assign(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(service.reassign(id, str(body.get("assignee"))));
    }

    @PostMapping("/{id}/notify")
    public R<NotifyMessage> notifyTask(
            @PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(service.notify(id, body == null ? "SMS" : str(body.get("channel"))));
    }

    @GetMapping("/{id}/messages")
    public R<List<NotifyMessage>> messages(@PathVariable Long id) {
        return R.ok(service.messages(id));
    }

    /** 手工触发自动任务生成；dealerCode 空表示全部经销商。 */
    @PostMapping("/generate")
    public R<Integer> generate(@RequestBody(required = false) Map<String, Object> body) {
        return R.ok(generator.generate(body == null ? null : str(body.get("dealerCode"))));
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
