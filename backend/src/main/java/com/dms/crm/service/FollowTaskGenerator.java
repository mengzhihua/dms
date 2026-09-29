package com.dms.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.dms.common.CodeGenerator;
import com.dms.crm.entity.FollowTask;
import com.dms.crm.mapper.FollowTaskMapper;
import com.dms.customer.entity.Customer;
import com.dms.customer.entity.Vehicle;
import com.dms.customer.entity.VehicleModel;
import com.dms.customer.mapper.CustomerMapper;
import com.dms.customer.mapper.VehicleMapper;
import com.dms.customer.mapper.VehicleModelMapper;
import com.dms.survey.entity.Complaint;
import com.dms.survey.mapper.ComplaintMapper;
import com.dms.workshop.entity.WorkOrder;
import com.dms.workshop.mapper.WorkOrderMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 跟进任务自动生成：保养到期提醒、售后回访、投诉跟进。
 * 依赖 dms_follow_task 的 UNIQUE(dealer_code,type,source_ref) 保证幂等。
 */
@Service
@RequiredArgsConstructor
public class FollowTaskGenerator {
    private static final Logger log = LoggerFactory.getLogger(FollowTaskGenerator.class);
    private static final int MAINT_WINDOW_DAYS = 15;
    private static final int FOLLOWUP_WINDOW_DAYS = 3;

    private final FollowTaskMapper taskMapper;
    private final VehicleMapper vehicleMapper;
    private final VehicleModelMapper modelMapper;
    private final CustomerMapper customerMapper;
    private final WorkOrderMapper orderMapper;
    private final ComplaintMapper complaintMapper;
    private final CodeGenerator codeGenerator;

    /** 每日定时生成（dms.crm.cron 可配置）。 */
    @Scheduled(cron = "${dms.crm.cron:0 0 8 * * ?}")
    public int daily() {
        int n = generate(null);
        if (n > 0) {
            log.info("跟进任务定时生成 {} 条", n);
        }
        return n;
    }

    /** 手工触发生成；dealerCode 为空时覆盖全部经销商。返回新建任务数。 */
    @Transactional
    public int generate(String dealerCode) {
        int n = 0;
        n += generateMaintenance(dealerCode);
        n += generateServiceFollowup(dealerCode);
        n += generateComplaintFollowup(dealerCode);
        return n;
    }

    /** 保养到期：上次保养日期+保养间隔月数 <= 今天+15天，或 距下次保养里程<=500km。 */
    int generateMaintenance(String dealerCode) {
        LocalDate horizon = LocalDate.now().plusDays(MAINT_WINDOW_DAYS);
        QueryWrapper<Vehicle> q = new QueryWrapper<>();
        if (dealerCode != null) {
            q.eq("dealer_code", dealerCode);
        }
        int n = 0;
        for (Vehicle v : vehicleMapper.selectList(q)) {
            Integer months = 6;
            if (v.getModelCode() != null) {
                VehicleModel m =
                        modelMapper.selectOne(
                                new QueryWrapper<VehicleModel>().eq("code", v.getModelCode()));
                if (m != null && m.getMaintenanceIntervalMonths() != null) {
                    months = m.getMaintenanceIntervalMonths();
                }
            }
            LocalDate due = null;
            String ref = null;
            if (v.getLastServiceDate() != null
                    && !v.getLastServiceDate().plusMonths(months).isAfter(horizon)) {
                due = v.getLastServiceDate().plusMonths(months);
                ref = v.getVin() + ":" + due.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            } else if (v.getNextServiceMileage() != null
                    && v.getMileage() != null
                    && v.getNextServiceMileage() - v.getMileage() <= 500) {
                due = horizon;
                // 里程触发：引用锚定下次保养里程，避免月份漂移导致重复任务
                ref = v.getVin() + ":KM" + v.getNextServiceMileage();
            }
            if (due == null) {
                continue;
            }
            FollowTask t = base(v.getDealerCode(), "MAINTENANCE_REMIND", ref);
            t.setVehicleId(v.getId());
            t.setCustomerId(v.getCustomerId());
            t.setVin(v.getVin());
            t.setPlateNo(v.getPlateNo());
            fillCustomer(t);
            t.setTitle("保养提醒-" + (v.getPlateNo() == null ? v.getVin() : v.getPlateNo()));
            t.setContent("车辆保养到期（计划日期 " + due + "），请邀约客户回店保养");
            t.setDueDate(due);
            n += insertIfAbsent(t);
        }
        return n;
    }

    /** 售后回访：近 3 天内已交车/关单的工单。 */
    int generateServiceFollowup(String dealerCode) {
        LocalDateTime since = LocalDateTime.now().minusDays(FOLLOWUP_WINDOW_DAYS);
        QueryWrapper<WorkOrder> q =
                new QueryWrapper<WorkOrder>()
                        .in("status", Arrays.asList("DELIVERED", "CLOSED"))
                        .ge("deliver_time", since);
        if (dealerCode != null) {
            q.eq("dealer_code", dealerCode);
        }
        int n = 0;
        for (WorkOrder o : orderMapper.selectList(q)) {
            n += insertIfAbsent(buildServiceFollowup(o));
        }
        return n;
    }

    /** 售后回访任务（供交车钩子和批量生成复用）。 */
    public FollowTask buildServiceFollowup(WorkOrder o) {
        FollowTask t = base(o.getDealerCode(), "SERVICE_FOLLOWUP", o.getOrderNo());
        t.setVehicleId(o.getVehicleId());
        t.setCustomerId(o.getCustomerId());
        t.setVin(o.getVin());
        t.setPlateNo(o.getPlateNo());
        fillCustomer(t);
        t.setTitle("售后回访-" + (o.getOrderNo() == null ? "" : o.getOrderNo()));
        t.setContent("工单 " + o.getOrderNo() + " 已交车，请在3天内回访客户满意度");
        LocalDate base = o.getDeliverTime() == null ? LocalDate.now() : o.getDeliverTime().toLocalDate();
        t.setDueDate(base.plusDays(FOLLOWUP_WINDOW_DAYS));
        return t;
    }

    /**
     * 交车钩子：立即为该工单生成售后回访任务（幂等）。
     * 不加事务注解：单条插入，若异常不能污染调用方事务（deliver 中已 try/catch）。
     */
    public void createForOrder(WorkOrder o) {
        if (o.getVin() != null) {
            // 交车视为本次保养完成：关闭该车所有待处理保养提醒，下一周期才能重新生成
            taskMapper.update(
                    null,
                    new UpdateWrapper<FollowTask>()
                            .eq("dealer_code", o.getDealerCode())
                            .eq("type", "MAINTENANCE_REMIND")
                            .eq("vin", o.getVin())
                            .eq("status", "PENDING")
                            .set("status", "DONE")
                            .set("result", "交车自动完成")
                            .set("done_at", LocalDateTime.now())
                            .set("updated_at", LocalDateTime.now()));
        }
        insertIfAbsent(buildServiceFollowup(o));
    }

    /** 投诉跟进：未关闭的投诉单。 */
    int generateComplaintFollowup(String dealerCode) {
        QueryWrapper<Complaint> q =
                new QueryWrapper<Complaint>().in("status", Arrays.asList("OPEN", "PROCESSING"));
        if (dealerCode != null) {
            q.eq("dealer_code", dealerCode);
        }
        int n = 0;
        for (Complaint c : complaintMapper.selectList(q)) {
            String ref =
                    c.getComplaintNo() == null ? "CMP-" + c.getId() : c.getComplaintNo();
            FollowTask t = base(c.getDealerCode(), "COMPLAINT_FOLLOWUP", ref);
            t.setCustomerId(c.getCustomerId());
            fillCustomer(t);
            t.setTitle("投诉跟进-" + ref);
            t.setContent(
                    "投诉 "
                            + ref
                            + " 待跟进："
                            + (c.getContent() == null
                                    ? ""
                                    : c.getContent().length() > 100
                                            ? c.getContent().substring(0, 100)
                                            : c.getContent()));
            t.setDueDate(LocalDate.now().plusDays(1));
            n += insertIfAbsent(t);
        }
        return n;
    }

    private FollowTask base(String dealerCode, String type, String ref) {
        FollowTask t = new FollowTask();
        t.setTaskNo(codeGenerator.next("FT"));
        t.setDealerCode(dealerCode);
        t.setType(type);
        t.setSource("AUTO");
        t.setSourceRef(ref);
        t.setStatus("PENDING");
        return t;
    }

    private void fillCustomer(FollowTask t) {
        if (t.getCustomerId() == null) {
            return;
        }
        Customer c = customerMapper.selectById(t.getCustomerId());
        if (c != null) {
            t.setCustomerName(c.getName());
            t.setPhone(c.getPhone());
        }
    }

    /** 按唯一键 (dealer_code,type,source_ref) 判重后插入；返回 1/0。 */
    private int insertIfAbsent(FollowTask t) {
        // 保养提醒：同一车辆已存在 PENDING 任务即跳过，不管 source_ref 是日期还是里程触发
        if ("MAINTENANCE_REMIND".equals(t.getType()) && t.getVin() != null) {
            Long pending =
                    taskMapper.selectCount(
                            new QueryWrapper<FollowTask>()
                                    .eq("dealer_code", t.getDealerCode())
                                    .eq("type", "MAINTENANCE_REMIND")
                                    .eq("vin", t.getVin())
                                    .eq("status", "PENDING"));
            if (pending != null && pending > 0) {
                return 0;
            }
        }
        Long cnt =
                taskMapper.selectCount(
                        new QueryWrapper<FollowTask>()
                                .eq("dealer_code", t.getDealerCode())
                                .eq("type", t.getType())
                                .eq(t.getSourceRef() != null, "source_ref", t.getSourceRef())
                                .isNull(t.getSourceRef() == null, "source_ref"));
        if (cnt != null && cnt > 0) {
            return 0;
        }
        try {
            taskMapper.insert(t);
            return 1;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            return 0;
        }
    }
}
