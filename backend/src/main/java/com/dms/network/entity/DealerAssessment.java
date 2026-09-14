package com.dms.network.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_dealer_assessment")
public class DealerAssessment extends BaseEntity {
    private String dealerCode;
    private String yearMonth;
    private BigDecimal salesScore;
    private BigDecimal serviceScore;
    private BigDecimal csiScore;
    private BigDecimal complianceScore;
    private BigDecimal total;
    private String grade;
}
