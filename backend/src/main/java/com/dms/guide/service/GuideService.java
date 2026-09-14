package com.dms.guide.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.common.BizException;
import com.dms.guide.entity.LaborItem;
import com.dms.guide.entity.RepairGuide;
import com.dms.guide.mapper.LaborItemMapper;
import com.dms.guide.mapper.RepairGuideMapper;
import com.dms.network.entity.Dealer;
import com.dms.network.mapper.DealerMapper;
import com.dms.parts.entity.Part;
import com.dms.parts.mapper.PartMapper;
import com.dms.parts.service.PartStockService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GuideService {
    private final RepairGuideMapper guideMapper;
    private final LaborItemMapper laborMapper;
    private final PartMapper partMapper;
    private final DealerMapper dealerMapper;
    private final PartStockService stockService;

    /** 智能推荐：打分排序，附工时明细与备件（含价格及在 dealerCode 处的可用量）。 */
    public List<Map<String, Object>> recommend(
            String modelCode, List<String> dtcCodes, String symptom, String dealerCode) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (RepairGuide g : guideMapper.selectList(null)) {
            int score = GuideMatcher.score(g, modelCode, dtcCodes, symptom);
            if (score <= 0) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("score", score);
            m.put("guide", g);
            m.put("labors", resolveLabors(g));
            m.put("parts", resolveParts(g, dealerCode));
            result.add(m);
        }
        result.sort(Comparator.comparingInt(a -> -((Integer) ((Map) a).get("score"))));
        return result;
    }

    /** 估价：工时费 = Σ标准工时 × 经销商工时单价；备件费 = Σ销售价×1（数量1）。 */
    public Map<String, Object> estimate(String guideCode, String dealerCode) {
        RepairGuide g =
                guideMapper.selectOne(new QueryWrapper<RepairGuide>().eq("code", guideCode));
        if (g == null) {
            throw new BizException("维修指导不存在: " + guideCode);
        }
        Dealer dealer = null;
        if (dealerCode != null) {
            dealer = dealerMapper.selectOne(new QueryWrapper<Dealer>().eq("code", dealerCode));
        }
        BigDecimal rate =
                dealer != null && dealer.getLaborRate() != null
                        ? dealer.getLaborRate()
                        : new BigDecimal("150");
        BigDecimal hours = BigDecimal.ZERO;
        List<Map<String, Object>> labors = resolveLabors(g);
        for (Map<String, Object> l : labors) {
            hours = hours.add((BigDecimal) l.get("standardHours"));
        }
        BigDecimal laborAmount = hours.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal partsAmount = BigDecimal.ZERO;
        List<Map<String, Object>> parts = resolveParts(g, dealerCode);
        for (Map<String, Object> p : parts) {
            if (p.get("salePrice") != null) {
                partsAmount = partsAmount.add((BigDecimal) p.get("salePrice"));
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("guideCode", guideCode);
        m.put("dealerCode", dealerCode);
        m.put("laborRate", rate);
        m.put("totalHours", hours);
        m.put("laborAmount", laborAmount);
        m.put("partsAmount", partsAmount);
        m.put("totalAmount", laborAmount.add(partsAmount));
        m.put("labors", labors);
        m.put("parts", parts);
        return m;
    }

    private List<Map<String, Object>> resolveLabors(RepairGuide g) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String code : GuideMatcher.splitList(g.getLaborItemCodes())) {
            LaborItem li =
                    laborMapper.selectOne(new QueryWrapper<LaborItem>().eq("code", code.trim()));
            if (li != null) {
                Map<String, Object> m = new HashMap<>();
                m.put("code", li.getCode());
                m.put("name", li.getName());
                m.put("standardHours", li.getStandardHours());
                list.add(m);
            }
        }
        return list;
    }

    private List<Map<String, Object>> resolveParts(RepairGuide g, String dealerCode) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String no : GuideMatcher.splitList(g.getPartNos())) {
            Part p = partMapper.selectOne(new QueryWrapper<Part>().eq("part_no", no.trim()));
            if (p != null) {
                Map<String, Object> m = new HashMap<>();
                m.put("partNo", p.getPartNo());
                m.put("name", p.getName());
                m.put("salePrice", p.getSalePrice());
                if (dealerCode != null) {
                    m.put("available", stockService.available(dealerCode, p.getPartNo()));
                }
                list.add(m);
            }
        }
        return list;
    }
}
