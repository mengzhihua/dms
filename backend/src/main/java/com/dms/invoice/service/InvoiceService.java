package com.dms.invoice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.auth.DataScope;
import com.dms.common.BizException;
import com.dms.common.CodeGenerator;
import com.dms.invoice.entity.Invoice;
import com.dms.invoice.entity.InvoiceLine;
import com.dms.invoice.entity.TaxConfig;
import com.dms.invoice.mapper.InvoiceLineMapper;
import com.dms.invoice.mapper.InvoiceMapper;
import com.dms.invoice.mapper.TaxConfigMapper;
import com.dms.workshop.entity.WorkOrder;
import com.dms.workshop.entity.WorkOrderLabor;
import com.dms.workshop.entity.WorkOrderPart;
import com.dms.workshop.mapper.WorkOrderLaborMapper;
import com.dms.workshop.mapper.WorkOrderMapper;
import com.dms.workshop.mapper.WorkOrderPartMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {
    /** 修理修配服务税收分类编码 */
    public static final String TAX_CODE_LABOR = "3040502000000000000";
    /** 零配件（运输设备类）税收分类编码 */
    public static final String TAX_CODE_PART = "1090511010000000000";

    private final InvoiceMapper invoiceMapper;
    private final InvoiceLineMapper lineMapper;
    private final TaxConfigMapper taxConfigMapper;
    private final WorkOrderMapper orderMapper;
    private final WorkOrderLaborMapper laborMapper;
    private final WorkOrderPartMapper partMapper;
    private final CodeGenerator codeGenerator;
    private final TaxInvoiceGateway gateway;

    public InvoiceService(
            InvoiceMapper invoiceMapper,
            InvoiceLineMapper lineMapper,
            TaxConfigMapper taxConfigMapper,
            WorkOrderMapper orderMapper,
            WorkOrderLaborMapper laborMapper,
            WorkOrderPartMapper partMapper,
            CodeGenerator codeGenerator,
            TaxInvoiceGateway gateway) {
        this.invoiceMapper = invoiceMapper;
        this.lineMapper = lineMapper;
        this.taxConfigMapper = taxConfigMapper;
        this.orderMapper = orderMapper;
        this.laborMapper = laborMapper;
        this.partMapper = partMapper;
        this.codeGenerator = codeGenerator;
        this.gateway = gateway;
    }

    @Configuration
    public static class GatewayConfig {
        @Bean
        public TaxInvoiceGateway taxInvoiceGateway(
                @Value("${dms.tax.provider:MOCK}") String provider,
                @Value("${dms.tax.mock-fail-rate:0}") double mockFailRate,
                @Value("${dms.tax.endpoint:}") String endpoint,
                @Value("${dms.tax.app-id:}") String appId,
                @Value("${dms.tax.app-secret:}") String appSecret) {
            if ("HTTP".equalsIgnoreCase(provider)) {
                return new HttpTaxAdapter(endpoint, appId, appSecret);
            }
            return new MockTaxAdapter(mockFailRate);
        }
    }

    private TaxConfig taxConfig(String dealerCode) {
        return taxConfigMapper.selectOne(
                new QueryWrapper<TaxConfig>().eq("dealer_code", dealerCode).last("LIMIT 1"));
    }

    private void fillSellerAndAmounts(Invoice inv, BigDecimal amount) {
        TaxConfig tc = taxConfig(inv.getDealerCode());
        BigDecimal rate =
                inv.getTaxRate() != null
                        ? inv.getTaxRate()
                        : tc != null && tc.getTaxRate() != null
                                ? tc.getTaxRate()
                                : new BigDecimal("0.13");
        inv.setTaxRate(rate);
        inv.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        inv.setTaxAmount(
                amount.divide(BigDecimal.ONE.add(rate), 10, RoundingMode.HALF_UP)
                        .multiply(rate)
                        .setScale(2, RoundingMode.HALF_UP));
        inv.setNetAmount(inv.getAmount().subtract(inv.getTaxAmount()));
        if (tc != null) {
            inv.setSellerName(tc.getSellerName());
            inv.setSellerTaxNo(tc.getSellerTaxNo());
        }
    }

    /** 结算时按工单创建 DRAFT 发票申请，行项目 = 工时 + 备件明细。 */
    @Transactional
    public Long createFromWorkOrder(
            WorkOrder o, String invoiceType, String buyerName, String buyerTaxNo) {
        Invoice inv = new Invoice();
        inv.setInvoiceNo(codeGenerator.next("INV"));
        inv.setOrderId(o.getId());
        inv.setDealerCode(o.getDealerCode());
        inv.setInvoiceType(invoiceType == null ? "ELECTRONIC" : invoiceType);
        inv.setBuyerName(buyerName == null ? "个人" : buyerName);
        inv.setBuyerTaxNo(buyerTaxNo);
        fillSellerAndAmounts(inv, o.getCustomerPayable());
        inv.setStatus("DRAFT");
        invoiceMapper.insert(inv);

        for (WorkOrderLabor l : laborMapper.selectList(
                new QueryWrapper<WorkOrderLabor>().eq("order_id", o.getId()))) {
            if (Boolean.TRUE.equals(l.getIsWarranty())) {
                continue;
            }
            InvoiceLine line = new InvoiceLine();
            line.setInvoiceId(inv.getId());
            line.setName(l.getName());
            line.setUnit("次");
            line.setQty(l.getHours());
            line.setUnitPrice(l.getRate());
            line.setAmount(l.getAmount());
            line.setTaxRate(inv.getTaxRate());
            line.setTaxAmount(
                    l.getAmount()
                            .divide(BigDecimal.ONE.add(inv.getTaxRate()), 10, RoundingMode.HALF_UP)
                            .multiply(inv.getTaxRate())
                            .setScale(2, RoundingMode.HALF_UP));
            line.setTaxCategoryCode(TAX_CODE_LABOR);
            lineMapper.insert(line);
        }
        for (WorkOrderPart p : partMapper.selectList(
                new QueryWrapper<WorkOrderPart>().eq("order_id", o.getId()))) {
            if (Boolean.TRUE.equals(p.getIsWarranty())) {
                continue;
            }
            InvoiceLine line = new InvoiceLine();
            line.setInvoiceId(inv.getId());
            line.setName(p.getName());
            line.setUnit("件");
            line.setQty(new BigDecimal(p.getQty()));
            line.setUnitPrice(p.getUnitPrice());
            line.setAmount(p.getAmount());
            line.setTaxRate(inv.getTaxRate());
            line.setTaxAmount(
                    p.getAmount()
                            .divide(BigDecimal.ONE.add(inv.getTaxRate()), 10, RoundingMode.HALF_UP)
                            .multiply(inv.getTaxRate())
                            .setScale(2, RoundingMode.HALF_UP));
            line.setTaxCategoryCode(TAX_CODE_PART);
            lineMapper.insert(line);
        }
        // 折扣行：保证 Σ行金额 = 发票含税金额（customerPayable 已扣折扣）
        if (o.getDiscountAmount() != null
                && o.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal neg = o.getDiscountAmount().negate();
            InvoiceLine line = new InvoiceLine();
            line.setInvoiceId(inv.getId());
            line.setName("折扣");
            line.setUnit("次");
            line.setQty(BigDecimal.ONE);
            line.setUnitPrice(neg);
            line.setAmount(neg);
            line.setTaxRate(inv.getTaxRate());
            line.setTaxAmount(
                    neg.divide(BigDecimal.ONE.add(inv.getTaxRate()), 10, RoundingMode.HALF_UP)
                            .multiply(inv.getTaxRate())
                            .setScale(2, RoundingMode.HALF_UP));
            line.setTaxCategoryCode(TAX_CODE_LABOR);
            lineMapper.insert(line);
        }
        return inv.getId();
    }

    /** 手工创建草稿。 */
    @Transactional
    public Invoice createManual(Invoice inv) {
        inv.setId(null);
        inv.setDealerCode(DataScope.effectiveDealer(inv.getDealerCode()));
        inv.setInvoiceNo(codeGenerator.next("INV"));
        fillSellerAndAmounts(inv, inv.getAmount() == null ? BigDecimal.ZERO : inv.getAmount());
        inv.setStatus("DRAFT");
        invoiceMapper.insert(inv);
        return inv;
    }

    /** 开具发票：幂等，已 ISSUED 直接返回。 */
    @Transactional
    public Invoice issue(Long id) {
        Invoice inv = mustGet(id);
        if ("ISSUED".equals(inv.getStatus())) {
            return inv;
        }
        List<InvoiceLine> lines = lines(id);
        // 原子抢占 ISSUING，防止并发重复开具
        if (invoiceMapper.markIssuing(id) == 0) {
            Invoice cur = mustGet(id);
            if ("ISSUED".equals(cur.getStatus())) {
                return cur;
            }
            throw new BizException("发票正在开具或状态不允许: " + cur.getStatus());
        }
        inv.setStatus("ISSUING");
        TaxInvoiceGateway.IssueResult r = gateway.issue(inv, lines);
        if (r.isSuccess()) {
            inv.setStatus("ISSUED");
            inv.setTaxInvoiceCode(r.getCode());
            inv.setTaxInvoiceNumber(r.getNumber());
            inv.setCheckCode(r.getCheckCode());
            inv.setPdfUrl(r.getPdfUrl());
            inv.setProviderRef(r.getProviderRef());
            inv.setIssuedTime(LocalDateTime.now());
            inv.setErrorMsg(null);
        } else {
            inv.setStatus("FAILED");
            inv.setErrorMsg(r.getErrorMsg());
        }
        invoiceMapper.updateById(inv);
        return inv;
    }

    /** 红冲：生成负数红字发票并把原票置为 RED_FLUSHED。 */
    @Transactional
    public Invoice redFlush(Long id) {
        Invoice orig = mustGet(id);
        if (!"ISSUED".equals(orig.getStatus())) {
            throw new BizException("只有已开具发票可以红冲");
        }
        // 原子把原票置为红冲中，防止并发重复红冲
        if (invoiceMapper.markRedFlushing(id) == 0) {
            throw new BizException("发票正在红冲或状态不允许: " + mustGet(id).getStatus());
        }
        Invoice red = new Invoice();
        red.setInvoiceNo(codeGenerator.next("INV"));
        red.setOrderId(orig.getOrderId());
        red.setSalesOrderId(orig.getSalesOrderId());
        red.setDealerCode(orig.getDealerCode());
        red.setInvoiceType(orig.getInvoiceType());
        red.setBuyerName(orig.getBuyerName());
        red.setBuyerTaxNo(orig.getBuyerTaxNo());
        red.setSellerName(orig.getSellerName());
        red.setSellerTaxNo(orig.getSellerTaxNo());
        red.setTaxRate(orig.getTaxRate());
        red.setAmount(orig.getAmount().negate());
        red.setTaxAmount(orig.getTaxAmount().negate());
        red.setNetAmount(orig.getNetAmount().negate());
        red.setRedOfInvoiceId(orig.getId());
        red.setStatus("ISSUING");
        invoiceMapper.insert(red);
        List<InvoiceLine> redLines = lines(orig.getId());
        TaxInvoiceGateway.IssueResult r = gateway.redFlush(orig, red, redLines);
        if (r.isSuccess()) {
            red.setStatus("ISSUED");
            red.setTaxInvoiceCode(r.getCode());
            red.setTaxInvoiceNumber(r.getNumber());
            red.setCheckCode(r.getCheckCode());
            red.setPdfUrl(r.getPdfUrl());
            red.setProviderRef(r.getProviderRef());
            red.setIssuedTime(LocalDateTime.now());
            orig.setStatus("RED_FLUSHED");
        } else {
            red.setStatus("FAILED");
            red.setErrorMsg(r.getErrorMsg());
            // 红冲失败恢复原票状态
            orig.setStatus("ISSUED");
        }
        invoiceMapper.updateById(red);
        invoiceMapper.updateById(orig);
        return red;
    }

    /** 预览：模拟发送给税控平台的报文内容。 */
    public Map<String, Object> preview(Long id) {
        Invoice inv = mustGet(id);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("invoice", inv);
        m.put("lines", lines(id));
        m.put("endpoint", "provider=" + gateway.getClass().getSimpleName());
        return m;
    }

    /** 异步回调：按 providerRef/发票号码回写状态。 */
    @Transactional
    public Invoice callback(String provider, Map<String, Object> body) {
        String ref = (String) body.get("providerRef");
        String number = (String) body.get("taxInvoiceNumber");
        Invoice inv = null;
        if (ref != null) {
            inv =
                    invoiceMapper.selectOne(
                            new QueryWrapper<Invoice>().eq("provider_ref", ref).last("LIMIT 1"));
        }
        if (inv == null && number != null) {
            inv =
                    invoiceMapper.selectOne(
                            new QueryWrapper<Invoice>()
                                    .eq("tax_invoice_number", number)
                                    .last("LIMIT 1"));
        }
        if (inv == null) {
            throw new BizException("找不到对应发票");
        }
        String status = (String) body.get("status");
        if (status != null) {
            if (!"ISSUED".equals(status)
                    && !"FAILED".equals(status)
                    && !"RED_FLUSHED".equals(status)) {
                throw new BizException("非法回调状态: " + status);
            }
            inv.setStatus(status);
            if ("ISSUED".equals(status)) {
                inv.setIssuedTime(LocalDateTime.now());
            }
            inv.setErrorMsg((String) body.get("errorMsg"));
            invoiceMapper.updateById(inv);
        }
        return inv;
    }

    private List<InvoiceLine> lines(Long invoiceId) {
        return lineMapper.selectList(
                new QueryWrapper<InvoiceLine>().eq("invoice_id", invoiceId).orderByAsc("id"));
    }

    private Invoice mustGet(Long id) {
        Invoice inv = invoiceMapper.selectById(id);
        if (inv == null) {
            throw new BizException("发票不存在: " + id);
        }
        DataScope.check(inv.getDealerCode());
        return inv;
    }
}
