package com.dms.guide.controller;

import com.dms.common.BaseCrudController;
import com.dms.guide.entity.TechnicalBulletin;
import com.dms.guide.mapper.TechnicalBulletinMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guide/bulletin")
public class TechnicalBulletinController extends BaseCrudController<TechnicalBulletin, TechnicalBulletinMapper> {
    public TechnicalBulletinController() {
        super(TechnicalBulletin.class);
    }

    protected String[] keywordColumns() {
        return new String[]{"code","title"};
    }
}
