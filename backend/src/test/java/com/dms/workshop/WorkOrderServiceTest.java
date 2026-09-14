package com.dms.workshop;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.common.CodeGenerator;
import com.dms.customer.mapper.VehicleMapper;
import com.dms.customer.mapper.VehicleModelMapper;
import com.dms.guide.mapper.LaborItemMapper;
import com.dms.guide.mapper.RepairGuideMapper;
import com.dms.invoice.service.InvoiceService;
import com.dms.network.mapper.BayMapper;
import com.dms.network.mapper.DealerMapper;
import com.dms.network.mapper.TechnicianMapper;
import com.dms.parts.mapper.PartMapper;
import com.dms.parts.service.PartStockService;
import com.dms.survey.service.SurveyService;
import com.dms.workshop.entity.WorkOrder;
import com.dms.workshop.entity.WorkOrderPart;
import com.dms.workshop.mapper.*;
import com.dms.workshop.service.WorkOrderService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 返工后再完工不得重复扣库存（consumedFlag 保护）。 */
class WorkOrderServiceTest {
    private WorkOrderService svc(WorkOrderMapper om, WorkOrderPartMapper pm, PartStockService ss) {
        return new WorkOrderService(
                om,
                mock(WorkOrderLaborMapper.class),
                pm,
                mock(WorkOrderLogMapper.class),
                mock(AppointmentMapper.class),
                mock(WarrantyClaimMapper.class),
                mock(TechnicianMapper.class),
                mock(BayMapper.class),
                mock(DealerMapper.class),
                mock(LaborItemMapper.class),
                mock(PartMapper.class),
                mock(RepairGuideMapper.class),
                mock(VehicleMapper.class),
                mock(VehicleModelMapper.class),
                ss,
                mock(CodeGenerator.class),
                mock(InvoiceService.class),
                mock(SurveyService.class));
    }

    private WorkOrderPart line(boolean reserved, boolean consumed) {
        WorkOrderPart p = new WorkOrderPart();
        p.setPartNo("P0001");
        p.setQty(2);
        p.setReservedFlag(reserved);
        p.setConsumedFlag(consumed);
        return p;
    }

    @Test
    void finishDoesNotDoubleConsumeAfterRework() {
        WorkOrderMapper om = mock(WorkOrderMapper.class);
        WorkOrderPartMapper pm = mock(WorkOrderPartMapper.class);
        PartStockService ss = mock(PartStockService.class);
        WorkOrder o = new WorkOrder();
        o.setId(1L);
        o.setDealerCode("D001");
        o.setOrderNo("WO1");
        o.setStatus("IN_REPAIR");
        when(om.selectById(1L)).thenReturn(o);
        // 第一次 finish：两行都已预留未消耗
        List<WorkOrderPart> first = Arrays.asList(line(true, false), line(true, false));
        // 第二次 finish（返工后）：行已被置为 consumed
        List<WorkOrderPart> second = Arrays.asList(line(false, true), line(false, true));
        when(pm.selectList(any(QueryWrapper.class))).thenReturn(first).thenReturn(second);

        WorkOrderService svc = svc(om, pm, ss);
        svc.finish(1L, "op");
        o.setStatus("IN_REPAIR"); // 模拟 QC_FAILED → 返工
        svc.finish(1L, "op");
        // consume 只在第一次执行，共 2 行各一次
        verify(ss, times(2)).consume(eq("D001"), eq("P0001"), eq(2), eq("WO"), eq("WO1"));
    }

    @Test
    void qcFailReoccupiesIdleResources() {
        WorkOrderMapper om = mock(WorkOrderMapper.class);
        WorkOrderPartMapper pm = mock(WorkOrderPartMapper.class);
        TechnicianMapper tm = mock(TechnicianMapper.class);
        BayMapper bm = mock(BayMapper.class);
        PartStockService ss = mock(PartStockService.class);
        WorkOrder o = new WorkOrder();
        o.setId(1L);
        o.setDealerCode("D001");
        o.setOrderNo("WO1");
        o.setStatus("QC_PENDING");
        o.setTechnicianCode("T001");
        o.setBayCode("B01");
        when(om.selectById(1L)).thenReturn(o);
        com.dms.network.entity.Technician t = new com.dms.network.entity.Technician();
        t.setCode("T001");
        t.setStatus("IDLE");
        com.dms.network.entity.Bay b = new com.dms.network.entity.Bay();
        b.setCode("B01");
        b.setStatus("IDLE");
        when(tm.selectOne(any(QueryWrapper.class))).thenReturn(t);
        when(bm.selectOne(any(QueryWrapper.class))).thenReturn(b);

        WorkOrderService svc =
                new WorkOrderService(
                        om,
                        mock(WorkOrderLaborMapper.class),
                        pm,
                        mock(WorkOrderLogMapper.class),
                        mock(AppointmentMapper.class),
                        mock(WarrantyClaimMapper.class),
                        tm,
                        bm,
                        mock(DealerMapper.class),
                        mock(LaborItemMapper.class),
                        mock(PartMapper.class),
                        mock(RepairGuideMapper.class),
                        mock(VehicleMapper.class),
                        mock(VehicleModelMapper.class),
                        ss,
                        mock(CodeGenerator.class),
                        mock(InvoiceService.class),
                        mock(SurveyService.class));
        WorkOrder r = svc.qc(1L, false, "不合格", "op");
        assertEquals("IN_REPAIR", r.getStatus());
        assertEquals("BUSY", t.getStatus());
        assertEquals("BUSY", b.getStatus());
    }
}
