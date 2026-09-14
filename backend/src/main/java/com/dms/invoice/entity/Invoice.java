package com.dms.invoice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_invoice")
public class Invoice extends BaseEntity {
    private String invoiceNo;
    private Long orderId;
    private Long salesOrderId;
    private String dealerCode;
    private String invoiceType; // NORMAL/SPECIAL/ELECTRONIC
    private String buyerName;
    private String buyerTaxNo;
    private String buyerAddress;
    private String buyerBank;
    private String sellerName;
    private String sellerTaxNo;
    private BigDecimal amount;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal netAmount;
    private String status; // DRAFT/ISSUING/ISSUED/RED_FLUSHED/FAILED
    private String taxInvoiceCode;
    private String taxInvoiceNumber;
    private String checkCode;
    private String pdfUrl;
    private LocalDateTime issuedTime;
    private String providerRef;
    private String errorMsg;
    private Long redOfInvoiceId;
}
