package com.dms.report.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 经营日报：每经销商每天一行的关键经营指标快照。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_daily_report")
public class DailyReport extends BaseEntity {
    private LocalDate reportDate;
    private String dealerCode;
    private String dealerName;
    private Integer checkIns;
    private Integer delivered;
    private Integer settledOrders;
    private BigDecimal revenue;
    private BigDecimal laborAmount;
    private BigDecimal partsAmount;
    private Integer salesOrders;
    private Integer salesDelivered;
    private BigDecimal salesAmount;
    private Integer partsIn;
    private Integer partsOut;
    private Integer shortageCount;
    private Integer surveys;
    private BigDecimal nps;
    private Integer complaints;
    private Integer claims;
    private BigDecimal claimAmount;
    private Integer poCount;
    private BigDecimal poAmount;
    private Integer pendingTasks;
    private LocalDateTime generatedAt;
}
