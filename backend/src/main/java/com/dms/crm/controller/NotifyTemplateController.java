package com.dms.crm.controller;

import com.dms.common.BaseCrudController;
import com.dms.crm.entity.NotifyTemplate;
import com.dms.crm.mapper.NotifyTemplateMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 通知模板维护：写权限仅 OEM/ADMIN（见 RolePolicy）。 */
@RestController
@RequestMapping("/api/crm/template")
public class NotifyTemplateController
        extends BaseCrudController<NotifyTemplate, NotifyTemplateMapper> {

    public NotifyTemplateController() {
        super(NotifyTemplate.class);
    }

    protected String[] keywordColumns() {
        return new String[] {"code", "name"};
    }
}
