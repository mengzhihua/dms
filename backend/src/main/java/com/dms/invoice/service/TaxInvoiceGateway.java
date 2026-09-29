package com.dms.invoice.service;

import com.dms.invoice.entity.Invoice;
import com.dms.invoice.entity.InvoiceLine;
import java.util.List;
import lombok.Data;

/** 税控/电子发票平台对接抽象（金税盘/税控盘/电子税局等）。 */
public interface TaxInvoiceGateway {
    IssueResult issue(Invoice invoice, List<InvoiceLine> lines);

    /** 异步查询开票结果（ISSUED/FAILED）；providerRef 可为空，requestId 为开票申请号（invoiceNo）。 */
    IssueResult query(String providerRef, String requestId);

    /** 红冲：针对已开具发票开具负数发票。 */
    IssueResult redFlush(Invoice original, Invoice redInvoice, List<InvoiceLine> redLines);

    @Data
    class IssueResult {
        private boolean success;
        private String code; // 发票代码
        private String number; // 发票号码
        private String checkCode; // 校验码
        private String pdfUrl;
        private String providerRef;
        private String errorMsg;
        /** true = 平台已受理，异步出票，需后续 query/回调确认。 */
        private boolean pending;
        /** true = 失败可重试（超时/网络/5xx），sync 不应置 FAILED。 */
        private boolean retryable;
    }
}
