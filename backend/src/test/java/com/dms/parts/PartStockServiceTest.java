package com.dms.parts;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.common.BizException;
import com.dms.parts.entity.PartStock;
import com.dms.parts.mapper.PartMapper;
import com.dms.parts.mapper.PartStockMapper;
import com.dms.parts.mapper.StockMovementMapper;
import com.dms.parts.service.PartStockService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class PartStockServiceTest {
    private PartStock s(long id, String batch, int qty, int reserved) {
        PartStock s = new PartStock();
        s.setId(id);
        s.setDealerCode("D001");
        s.setPartNo("P0001");
        s.setLocation("A-01");
        s.setBatchNo(batch);
        s.setQty(qty);
        s.setReservedQty(reserved);
        return s;
    }

    private PartStockService svc(PartStockMapper sm, List<PartStock> stocks) {
        when(sm.selectList(any(QueryWrapper.class))).thenReturn(stocks);
        StockMovementMapper mm = mock(StockMovementMapper.class);
        PartMapper pm = mock(PartMapper.class);
        return new PartStockService(sm, mm, pm);
    }

    @Test
    void reserveFifoByBatch() {
        PartStockMapper sm = mock(PartStockMapper.class);
        List<PartStock> stocks = Arrays.asList(s(1, "B202301", 5, 0), s(2, "B202302", 10, 0));
        when(sm.atomicReserve(anyLong(), anyInt())).thenReturn(1);
        PartStockService svc = svc(sm, stocks);
        svc.reserve("D001", "P0001", 8, "WO", "WO1");
        // FIFO: 第一批拿满5，第二批拿3
        verify(sm).atomicReserve(1L, 5);
        verify(sm).atomicReserve(2L, 3);
    }

    @Test
    void reserveInsufficient() {
        PartStockMapper sm = mock(PartStockMapper.class);
        List<PartStock> stocks = Arrays.asList(s(1, "B202301", 5, 3));
        when(sm.atomicReserve(anyLong(), anyInt())).thenReturn(1);
        PartStockService svc = svc(sm, stocks);
        BizException e =
                assertThrows(BizException.class, () -> svc.reserve("D001", "P0001", 5, "WO", "WO1"));
        assertTrue(e.getMessage().contains("库存不足"));
    }

    @Test
    void consumeReserved() {
        PartStockMapper sm = mock(PartStockMapper.class);
        List<PartStock> stocks = Arrays.asList(s(1, "B202301", 10, 6));
        when(sm.atomicConsume(anyLong(), anyInt())).thenReturn(1);
        PartStockService svc = svc(sm, stocks);
        svc.consume("D001", "P0001", 4, "WO", "WO1");
        verify(sm).atomicConsume(1L, 4);
        assertThrows(BizException.class, () -> svc.consume("D001", "P0001", 7, "WO", "WO1"));
    }
}
