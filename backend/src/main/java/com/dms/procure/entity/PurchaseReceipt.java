package com.dms.procure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_purchase_receipt")
public class PurchaseReceipt extends BaseEntity {
    private String receiptNo;
    private Long orderId;
    private String dealerCode;
    private String location;
    private String batchNo;
}
