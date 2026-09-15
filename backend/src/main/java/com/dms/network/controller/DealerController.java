package com.dms.network.controller;

import com.dms.common.BaseCrudController;
import com.dms.network.entity.Dealer;
import com.dms.network.mapper.DealerMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/network/dealer")
public class DealerController extends BaseCrudController<Dealer, DealerMapper> {
    public DealerController() {
        super(Dealer.class);
    }

    protected String[] keywordColumns() {
        return new String[]{"code","name"};
    }

    /** Dealer 没有 dealer_code 列，用自身 code 做数据范围。 */
    @Override
    protected String scopeColumn() {
        return "code";
    }
}
