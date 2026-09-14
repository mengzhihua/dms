package com.dms.oms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 备件补货单(DMS -> OMS 渠道订单)。
 * 状态:DRAFT(草稿) -> PUSHED(已下单 OMS) -> SHIPPED(OMS 已发货) -> RECEIVED(已入库) ;
 * DRAFT/PUSHED -> CANCELLED ; 下单失败保留 DRAFT 并记录 lastError。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_replenish_order")
public class ReplenishOrder extends BaseEntity {
    private String replenishNo;
    private String dealerCode;
    private String shopCode;
    private String status;
    private String source;
    /** JSON: [{partNo,name,qty,price}] */
    private String items;
    private String omsOrderNo;
    private String omsStatus;
    private String warehouseCode;
    private String carrierCode;
    private String trackingNo;
    private String location;
    private String lastError;
    private LocalDateTime pushedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime syncedAt;
}
