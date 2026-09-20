package com.dms.invoice.service;

import com.dms.invoice.entity.Invoice;
import com.dms.invoice.entity.InvoiceLine;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 发送给税控平台的请求报文（不直接暴露 Invoice 实体）。 */
@Data
public class TaxRequest {
    private String requestId; // = invoiceNo
    private String invoiceType;
    private Party buyer;
    private Party seller;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private BigDecimal netAmount;
    private BigDecimal taxRate;
    private List<Line> lines;
    private Ref redOf; // 仅红冲：原票的发票代码/号码
    private String callbackUrl;
    private String remark;

    @Data
    public static class Party {
        private String name;
        private String taxNo;
        private String address;
        private String bank;
    }

    @Data
    public static class Line {
        private String name;
        private String unit;
        private BigDecimal qty;
        private BigDecimal unitPrice;
        private BigDecimal amount;
        private BigDecimal taxRate;
        private BigDecimal taxAmount;
        private String taxCategoryCode;
    }

    @Data
    public static class Ref {
        private String code;
        private String number;
    }

    public static TaxRequest of(Invoice inv, List<InvoiceLine> lines, String callbackUrl) {
        TaxRequest r = new TaxRequest();
        r.requestId = inv.getInvoiceNo();
        r.invoiceType = inv.getInvoiceType();
        Party b = new Party();
        b.name = inv.getBuyerName();
        b.taxNo = inv.getBuyerTaxNo();
        b.address = inv.getBuyerAddress();
        b.bank = inv.getBuyerBank();
        r.buyer = b;
        Party s = new Party();
        s.name = inv.getSellerName();
        s.taxNo = inv.getSellerTaxNo();
        r.seller = s;
        r.amount = inv.getAmount();
        r.taxAmount = inv.getTaxAmount();
        r.netAmount = inv.getNetAmount();
        r.taxRate = inv.getTaxRate();
        r.lines = new ArrayList<>();
        for (InvoiceLine l : lines) {
            Line x = new Line();
            x.name = l.getName();
            x.unit = l.getUnit();
            x.qty = l.getQty();
            x.unitPrice = l.getUnitPrice();
            x.amount = l.getAmount();
            x.taxRate = l.getTaxRate();
            x.taxAmount = l.getTaxAmount();
            x.taxCategoryCode = l.getTaxCategoryCode();
            r.lines.add(x);
        }
        r.callbackUrl = callbackUrl;
        r.remark = inv.getRemark();
        return r;
    }
}
