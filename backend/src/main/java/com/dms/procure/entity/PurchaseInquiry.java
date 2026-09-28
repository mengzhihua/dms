package com.dms.procure.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_purchase_inquiry")
public class PurchaseInquiry extends BaseEntity {
    private String inquiryNo;
    private String dealerCode;
    private String title;
    /** DRAFT/SENT/QUOTED/ORDERED/CLOSED */
    private String status;
    private LocalDate expectDate;
    private LocalDateTime quotedAt;
    private String oemRemark;

    /** 创建时随单提交的明细行 [{partNo, qty, targetPrice?}]，不入库。 */
    @TableField(exist = false)
    private java.util.List<java.util.Map<String, Object>> lines;
}
