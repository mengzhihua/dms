package com.dms.parts.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.common.BizException;
import com.dms.parts.entity.Part;
import com.dms.parts.entity.PartStock;
import com.dms.parts.entity.StockMovement;
import com.dms.parts.mapper.PartMapper;
import com.dms.parts.mapper.PartStockMapper;
import com.dms.parts.mapper.StockMovementMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 备件库存服务：借鉴富勒/SAP EWM 的库位+批次+预留思路。
 * 所有扣减均使用带条件的原子 UPDATE，保证并发安全。
 */
@Service
@RequiredArgsConstructor
public class PartStockService {
    private final PartStockMapper stockMapper;
    private final StockMovementMapper movementMapper;
    private final PartMapper partMapper;

    private void log(
            String dealerCode,
            String partNo,
            String type,
            int qty,
            String refType,
            String refNo,
            String location,
            String batchNo) {
        StockMovement m = new StockMovement();
        m.setDealerCode(dealerCode);
        m.setPartNo(partNo);
        m.setType(type);
        m.setQty(qty);
        m.setRefType(refType);
        m.setRefNo(refNo);
        m.setLocation(location);
        m.setBatchNo(batchNo);
        movementMapper.insert(m);
    }

    /** 入库：按 (dealer,partNo,location,batchNo) 累加，不存在则新建。 */
    @Transactional
    public PartStock inbound(
            String dealerCode, String partNo, String location, String batchNo, int qty) {
        if (qty <= 0) {
            throw new BizException("入库数量必须大于0");
        }
        PartStock s = find(dealerCode, partNo, location, batchNo);
        if (s == null) {
            s = new PartStock();
            s.setDealerCode(dealerCode);
            s.setPartNo(partNo);
            s.setLocation(location);
            s.setBatchNo(batchNo);
            s.setQty(qty);
            s.setReservedQty(0);
            stockMapper.insert(s);
        } else {
            s.setQty(s.getQty() + qty);
            stockMapper.updateById(s);
        }
        log(dealerCode, partNo, "IN", qty, null, null, location, batchNo);
        return s;
    }

    public PartStock find(String dealerCode, String partNo, String location, String batchNo) {
        return stockMapper.selectOne(
                new QueryWrapper<PartStock>()
                        .eq("dealer_code", dealerCode)
                        .eq("part_no", partNo)
                        .eq("location", location)
                        .eq("batch_no", batchNo));
    }

    public int available(String dealerCode, String partNo) {
        int sum = 0;
        for (PartStock s : stocksOf(dealerCode, partNo)) {
            sum += s.getQty() - s.getReservedQty();
        }
        return sum;
    }

    private List<PartStock> stocksOf(String dealerCode, String partNo) {
        // FIFO：按批次号升序（批次号含日期前缀）
        return stockMapper.selectList(
                new QueryWrapper<PartStock>()
                        .eq("dealer_code", dealerCode)
                        .eq("part_no", partNo)
                        .orderByAsc("batch_no")
                        .orderByAsc("id"));
    }

    /** 预留：按批次 FIFO 逐行原子扣减，不足则整体抛错回滚。返回预留批次明细。 */
    @Transactional
    public List<PartStock> reserve(
            String dealerCode, String partNo, int qty, String refType, String refNo) {
        if (qty <= 0) {
            throw new BizException("预留数量必须大于0");
        }
        int need = qty;
        List<PartStock> used = new ArrayList<>();
        for (PartStock s : stocksOf(dealerCode, partNo)) {
            if (need <= 0) {
                break;
            }
            int avail = s.getQty() - s.getReservedQty();
            if (avail <= 0) {
                continue;
            }
            int take = Math.min(avail, need);
            if (stockMapper.atomicReserve(s.getId(), take) == 0) {
                throw new BizException("备件 " + partNo + " 库存预留冲突，请重试");
            }
            log(dealerCode, partNo, "RESERVE", take, refType, refNo, s.getLocation(), s.getBatchNo());
            used.add(s);
            need -= take;
        }
        if (need > 0) {
            throw new BizException(
                    "备件 " + partNo + " 库存不足：缺口 " + need + "（可用 "
                            + available(dealerCode, partNo) + "）");
        }
        return used;
    }

    /** 释放预留（FIFO 反序归还，按行原子 UPDATE）。 */
    @Transactional
    public void release(String dealerCode, String partNo, int qty, String refType, String refNo) {
        int need = qty;
        List<PartStock> stocks = stocksOf(dealerCode, partNo);
        for (int i = stocks.size() - 1; i >= 0 && need > 0; i--) {
            PartStock s = stocks.get(i);
            int back = Math.min(s.getReservedQty(), need);
            if (back <= 0) {
                continue;
            }
            if (stockMapper.atomicRelease(s.getId(), back) == 0) {
                throw new BizException("备件 " + partNo + " 释放预留冲突");
            }
            log(dealerCode, partNo, "RELEASE", back, refType, refNo, s.getLocation(), s.getBatchNo());
            need -= back;
        }
        if (need > 0) {
            throw new BizException("备件 " + partNo + " 可释放预留不足 " + need);
        }
    }

    /** 消耗：预留转为出库。 */
    @Transactional
    public void consume(String dealerCode, String partNo, int qty, String refType, String refNo) {
        int need = qty;
        for (PartStock s : stocksOf(dealerCode, partNo)) {
            if (need <= 0) {
                break;
            }
            int take = Math.min(Math.min(s.getReservedQty(), s.getQty()), need);
            if (take <= 0) {
                continue;
            }
            if (stockMapper.atomicConsume(s.getId(), take) == 0) {
                throw new BizException("备件 " + partNo + " 出库冲突");
            }
            log(dealerCode, partNo, "OUT", take, refType, refNo, s.getLocation(), s.getBatchNo());
            need -= take;
        }
        if (need > 0) {
            throw new BizException("备件 " + partNo + " 预留不足，无法出库 " + need);
        }
    }

    /** 缺货预警：各经销商下可用量低于 minStock 的备件。 */
    public List<java.util.Map<String, Object>> shortage() {
        List<java.util.Map<String, Object>> result = new ArrayList<>();
        for (Part p : partMapper.selectList(null)) {
            java.util.Set<String> dealers = new java.util.HashSet<>();
            for (PartStock s : stockMapper.selectList(
                    new QueryWrapper<PartStock>().eq("part_no", p.getPartNo()))) {
                dealers.add(s.getDealerCode());
            }
            for (String d : dealers) {
                int avail = available(d, p.getPartNo());
                if (p.getMinStock() != null && avail < p.getMinStock()) {
                    java.util.Map<String, Object> row = new java.util.HashMap<>();
                    row.put("dealerCode", d);
                    row.put("partNo", p.getPartNo());
                    row.put("name", p.getName());
                    row.put("available", avail);
                    row.put("minStock", p.getMinStock());
                    result.add(row);
                }
            }
        }
        return result;
    }
}
