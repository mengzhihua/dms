package com.dms.procure.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.dms.auth.DataScope;
import com.dms.common.BizException;
import com.dms.common.CodeGenerator;
import com.dms.procure.entity.PurchaseOrder;
import com.dms.procure.entity.PurchaseStatement;
import com.dms.procure.mapper.PurchaseOrderMapper;
import com.dms.procure.mapper.PurchaseStatementMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 采购对账：OEM 按月归集已到货/已关闭采购单，确认后付款。 */
@Service
@RequiredArgsConstructor
public class PurchaseStatementService {
    private final PurchaseStatementMapper mapper;
    private final PurchaseOrderMapper orderMapper;
    private final CodeGenerator codes;

    private PurchaseStatement mustGet(Long id) {
        PurchaseStatement s = mapper.selectById(id);
        if (s == null) {
            throw new BizException("对账单不存在");
        }
        return s;
    }

    /**
     * 生成对账单：归集该经销商 RECEIVED/CLOSED 且未对账的采购单；
     * period 限定关闭/更新月份（closedAt 为空回退 updatedAt）；
     * 条件 UPDATE 防并发重复归集，无可挂账单据整体回滚。
     */
    @Transactional
    public PurchaseStatement generate(Map<String, Object> body) {
        String dealerCode = body == null || body.get("dealerCode") == null
                ? null
                : String.valueOf(body.get("dealerCode")).trim();
        if (dealerCode == null || dealerCode.isEmpty()) {
            throw new BizException("请选择经销商");
        }
        DataScope.check(dealerCode);
        String period = body.get("period") == null ? null : String.valueOf(body.get("period")).trim();
        QueryWrapper<PurchaseOrder> q = new QueryWrapper<>();
        q.eq("dealer_code", dealerCode)
                .in("status", "RECEIVED", "CLOSED")
                .isNull("statement_id")
                .orderByAsc("id");
        List<PurchaseOrder> orders = orderMapper.selectList(q);
        if (period != null && !period.isEmpty()) {
            YearMonth ym = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyy-MM"));
            orders.removeIf(o -> {
                LocalDateTime t = o.getClosedAt() != null ? o.getClosedAt() : o.getUpdatedAt();
                return t == null || !YearMonth.from(t).equals(ym);
            });
        }
        if (orders.isEmpty()) {
            throw new BizException("该经销商无待对账的采购单");
        }
        PurchaseStatement s = new PurchaseStatement();
        s.setStatementNo(codes.next("PS"));
        s.setDealerCode(dealerCode);
        s.setPeriod(period);
        s.setStatus("DRAFT");
        mapper.insert(s);

        BigDecimal total = BigDecimal.ZERO;
        int attached = 0;
        for (PurchaseOrder o : orders) {
            if (orderMapper.attachToStatement(s.getId(), o.getId()) == 1) {
                attached++;
                total = total.add(
                        o.getReceivedAmount() == null ? BigDecimal.ZERO : o.getReceivedAmount());
            }
        }
        if (attached == 0) {
            throw new BizException("该经销商无待对账的采购单");
        }
        s.setOrderCount(attached);
        s.setTotalAmount(total);
        mapper.updateById(s);
        return s;
    }

    @Transactional
    public PurchaseStatement confirm(Long id) {
        PurchaseStatement s = mustGet(id);
        if (!"DRAFT".equals(s.getStatus())) {
            throw new BizException("仅草稿对账单可确认");
        }
        s.setStatus("CONFIRMED");
        mapper.updateById(s);
        return s;
    }

    @Transactional
    public PurchaseStatement pay(Long id) {
        PurchaseStatement s = mustGet(id);
        if (!"CONFIRMED".equals(s.getStatus())) {
            throw new BizException("仅已确认的对账单可付款");
        }
        s.setStatus("PAID");
        s.setPaidAt(LocalDateTime.now());
        mapper.updateById(s);
        return s;
    }

    /** 删除草稿对账单：归还其采购单（清 statement_id）。 */
    @Transactional
    public void delete(Long id) {
        PurchaseStatement s = mustGet(id);
        DataScope.check(s.getDealerCode());
        if (!"DRAFT".equals(s.getStatus())) {
            throw new BizException("仅草稿对账单可删除");
        }
        for (PurchaseOrder o : orders(id)) {
            UpdateWrapper<PurchaseOrder> u = new UpdateWrapper<>();
            u.eq("id", o.getId()).set("statement_id", null);
            orderMapper.update(null, u);
        }
        mapper.deleteById(id);
    }

    public List<PurchaseOrder> orders(Long id) {
        PurchaseStatement s = mustGet(id);
        DataScope.check(s.getDealerCode());
        return orderMapper.selectList(
                new QueryWrapper<PurchaseOrder>().eq("statement_id", id).orderByAsc("id"));
    }
}
