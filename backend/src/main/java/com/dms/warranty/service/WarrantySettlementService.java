package com.dms.warranty.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.common.BizException;
import com.dms.common.CodeGenerator;
import com.dms.warranty.entity.WarrantyClaim;
import com.dms.warranty.entity.WarrantySettlement;
import com.dms.warranty.mapper.WarrantyClaimMapper;
import com.dms.warranty.mapper.WarrantySettlementMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 索赔结算批次：OEM 按月归集已核准索赔单，确认后付款。 */
@Service
@RequiredArgsConstructor
public class WarrantySettlementService {
    private final WarrantySettlementMapper mapper;
    private final WarrantyClaimMapper claimMapper;
    private final CodeGenerator codeGenerator;

    private WarrantySettlement mustGet(Long id) {
        WarrantySettlement s = mapper.selectById(id);
        if (s == null) {
            throw new BizException("结算单不存在");
        }
        return s;
    }

    /**
     * 生成结算批次：归集该经销商 APPROVED 且未结算的索赔单；
     * period(yyyy-MM) 非空时限定核准月份。无单可结则报错。
     */
    @Transactional
    public WarrantySettlement generate(Map<String, Object> body) {
        String dealerCode = body == null || body.get("dealerCode") == null
                ? null
                : String.valueOf(body.get("dealerCode")).trim();
        if (dealerCode == null || dealerCode.isEmpty()) {
            throw new BizException("请选择经销商");
        }
        String period = body.get("period") == null ? null : String.valueOf(body.get("period")).trim();
        QueryWrapper<WarrantyClaim> q = new QueryWrapper<>();
        q.eq("dealer_code", dealerCode)
                .eq("status", WarrantyClaimService.APPROVED)
                .isNull("settlement_id")
                .orderByAsc("id");
        List<WarrantyClaim> claims = claimMapper.selectList(q);
        if (period != null && !period.isEmpty()) {
            YearMonth ym = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyy-MM"));
            claims.removeIf(c -> c.getApprovedAt() == null
                    || !YearMonth.from(c.getApprovedAt()).equals(ym));
        }
        if (claims.isEmpty()) {
            throw new BizException("该经销商无待结算的已核准索赔单");
        }
        WarrantySettlement s = new WarrantySettlement();
        s.setSettlementNo(codeGenerator.next("WS"));
        s.setDealerCode(dealerCode);
        s.setPeriod(period);
        s.setClaimCount(claims.size());
        s.setTotalAmount(claims.stream()
                .map(c -> c.getApprovedAmount() == null ? BigDecimal.ZERO : c.getApprovedAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        s.setStatus("DRAFT");
        mapper.insert(s);
        for (WarrantyClaim c : claims) {
            c.setSettlementId(s.getId());
            c.setStatus(WarrantyClaimService.SETTLED);
            claimMapper.updateById(c);
        }
        return s;
    }

    @Transactional
    public WarrantySettlement confirm(Long id) {
        WarrantySettlement s = mustGet(id);
        if (!"DRAFT".equals(s.getStatus())) {
            throw new BizException("仅草稿结算单可确认");
        }
        s.setStatus("CONFIRMED");
        mapper.updateById(s);
        return s;
    }

    /** 付款：结算单 PAID，其下索赔单全部 PAID。 */
    @Transactional
    public WarrantySettlement pay(Long id) {
        WarrantySettlement s = mustGet(id);
        if (!"CONFIRMED".equals(s.getStatus())) {
            throw new BizException("仅已确认的结算单可付款");
        }
        s.setStatus("PAID");
        s.setPaidAt(LocalDateTime.now());
        mapper.updateById(s);
        for (WarrantyClaim c : claims(id)) {
            c.setStatus(WarrantyClaimService.PAID);
            claimMapper.updateById(c);
        }
        return s;
    }

    public List<WarrantyClaim> claims(Long id) {
        return claimMapper.selectList(
                new QueryWrapper<WarrantyClaim>().eq("settlement_id", id).orderByAsc("id"));
    }
}
