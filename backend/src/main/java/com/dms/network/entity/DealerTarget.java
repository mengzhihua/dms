package com.dms.network.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_dealer_target")
public class DealerTarget extends BaseEntity {
    private String dealerCode;
    private String yearMonth;
    private Integer salesTarget;
    private Integer serviceTarget;
    private BigDecimal revenueTarget;
}
