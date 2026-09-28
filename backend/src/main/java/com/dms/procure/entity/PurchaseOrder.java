package com.dms.procure.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_purchase_order")
public class PurchaseOrder extends BaseEntity {
    private String poNo;
    private String dealerCode;
    private Long inquiryId;
    private String source; // MANUAL/INQUIRY/SHORTAGE
    /** DRAFT/SUBMITTED/CONFIRMED/PARTIAL_RECEIVED/RECEIVED/CLOSED/CANCELLED */
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal receivedAmount;
    private LocalDate expectDate;
    private String oemOrderNo;
    private String oemRemark;
    private Long statementId;
    private LocalDateTime submittedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime closedAt;

    /** 创建时随单提交的明细行 [{partNo, qty, unitPrice?}]，不入库。 */
    @TableField(exist = false)
    private java.util.List<java.util.Map<String, Object>> lines;
}
