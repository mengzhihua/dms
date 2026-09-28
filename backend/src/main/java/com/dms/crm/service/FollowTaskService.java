package com.dms.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.auth.DataScope;
import com.dms.common.BizException;
import com.dms.common.CodeGenerator;
import com.dms.crm.entity.FollowTask;
import com.dms.crm.entity.NotifyMessage;
import com.dms.crm.mapper.FollowTaskMapper;
import com.dms.customer.entity.Customer;
import com.dms.customer.entity.Vehicle;
import com.dms.customer.mapper.CustomerMapper;
import com.dms.customer.mapper.VehicleMapper;
import com.dms.network.entity.Dealer;
import com.dms.network.mapper.DealerMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 客户跟进任务：手工创建、完成/取消/改派、按模板通知客户。 */
@Service
@RequiredArgsConstructor
public class FollowTaskService {
    private final FollowTaskMapper mapper;
    private final CustomerMapper customerMapper;
    private final VehicleMapper vehicleMapper;
    private final DealerMapper dealerMapper;
    private final NotifyService notifyService;
    private final CodeGenerator codeGenerator;

    private static final Map<String, String> TYPE_TEMPLATE = new HashMap<>();

    static {
        TYPE_TEMPLATE.put("MAINTENANCE_REMIND", "MAINT_REMIND");
        TYPE_TEMPLATE.put("SERVICE_FOLLOWUP", "SERVICE_FOLLOWUP");
        TYPE_TEMPLATE.put("COMPLAINT_FOLLOWUP", "COMPLAINT_FOLLOWUP");
        TYPE_TEMPLATE.put("BIRTHDAY", "BIRTHDAY");
        TYPE_TEMPLATE.put("SALES_FOLLOWUP", "SALES_FOLLOWUP");
        TYPE_TEMPLATE.put("RENEWAL", "RENEWAL");
    }

    private FollowTask mustGet(Long id) {
        FollowTask t = mapper.selectById(id);
        if (t == null) {
            throw new BizException("跟进任务不存在");
        }
        return t;
    }

    private FollowTask scoped(Long id) {
        FollowTask t = mustGet(id);
        DataScope.check(t.getDealerCode());
        return t;
    }

    /** 手工创建：补全客户/车辆冗余字段，经销商 = effectiveDealer。 */
    @Transactional
    public FollowTask create(FollowTask body) {
        FollowTask t = new FollowTask();
        t.setTaskNo(codeGenerator.next("FT"));
        t.setDealerCode(DataScope.effectiveDealer(body.getDealerCode()));
        t.setCustomerId(body.getCustomerId());
        t.setVehicleId(body.getVehicleId());
        t.setVin(body.getVin());
        t.setPlateNo(body.getPlateNo());
        t.setCustomerName(body.getCustomerName());
        t.setPhone(body.getPhone());
        if (t.getCustomerId() != null) {
            Customer c = customerMapper.selectById(t.getCustomerId());
            if (c == null) {
                throw new BizException("客户不存在");
            }
            DataScope.check(c.getDealerCode());
            if (t.getCustomerName() == null) {
                t.setCustomerName(c.getName());
            }
            if (t.getPhone() == null) {
                t.setPhone(c.getPhone());
            }
        }
        if (t.getVehicleId() != null) {
            Vehicle v = vehicleMapper.selectById(t.getVehicleId());
            if (v == null) {
                throw new BizException("车辆不存在");
            }
            DataScope.check(v.getDealerCode());
            if (t.getVin() == null) {
                t.setVin(v.getVin());
            }
            if (t.getPlateNo() == null) {
                t.setPlateNo(v.getPlateNo());
            }
        }
        t.setType(body.getType() == null ? "MANUAL" : body.getType());
        t.setSource("MANUAL");
        t.setSourceRef(null);
        t.setTitle(body.getTitle());
        t.setContent(body.getContent());
        t.setDueDate(body.getDueDate());
        t.setAssignee(body.getAssignee());
        t.setStatus("PENDING");
        t.setRemark(body.getRemark());
        mapper.insert(t);
        return t;
    }

    @Transactional
    public FollowTask complete(Long id, String result) {
        FollowTask t = scoped(id);
        if (!"PENDING".equals(t.getStatus())) {
            throw new BizException("仅待处理任务可完成");
        }
        t.setStatus("DONE");
        t.setResult(result);
        t.setDoneAt(LocalDateTime.now());
        mapper.updateById(t);
        return t;
    }

    @Transactional
    public FollowTask cancel(Long id) {
        FollowTask t = scoped(id);
        if (!"PENDING".equals(t.getStatus())) {
            throw new BizException("仅待处理任务可取消");
        }
        t.setStatus("CANCELLED");
        mapper.updateById(t);
        return t;
    }

    @Transactional
    public FollowTask reassign(Long id, String assignee) {
        FollowTask t = scoped(id);
        if (assignee == null || assignee.trim().isEmpty()) {
            throw new BizException("指派人必填");
        }
        t.setAssignee(assignee);
        mapper.updateById(t);
        return t;
    }

    /** 按任务类型映射模板发送通知；客户无手机号报错。 */
    public NotifyMessage notify(Long id, String channel) {
        FollowTask t = scoped(id);
        if (t.getPhone() == null || t.getPhone().trim().isEmpty()) {
            throw new BizException("客户无手机号");
        }
        String tplCode = TYPE_TEMPLATE.get(t.getType());
        if (tplCode == null) {
            throw new BizException("该任务类型无通知模板: " + t.getType());
        }
        // 微信渠道优先使用 _WX 后缀模板（存在且启用时）
        if ("WECHAT".equals(channel) && notifyService.hasTemplate(tplCode + "_WX")) {
            tplCode = tplCode + "_WX";
        }
        Map<String, String> vars = new HashMap<>();
        vars.put("name", t.getCustomerName() == null ? "" : t.getCustomerName());
        vars.put("plate", t.getPlateNo() == null ? "" : t.getPlateNo());
        vars.put("date", t.getDueDate() == null ? LocalDate.now().toString() : t.getDueDate().toString());
        vars.put("dealer", dealerName(t.getDealerCode()));
        vars.put("mileage", vehicleMileage(t));
        return notifyService.send(
                t.getDealerCode(),
                channel == null ? "SMS" : channel,
                t.getPhone(),
                tplCode,
                vars,
                "FOLLOW_TASK",
                t.getId());
    }

    private String dealerName(String dealerCode) {
        Dealer d =
                dealerCode == null
                        ? null
                        : dealerMapper.selectOne(
                                new QueryWrapper<Dealer>().eq("code", dealerCode));
        return d == null ? (dealerCode == null ? "" : dealerCode) : d.getName();
    }

    private String vehicleMileage(FollowTask t) {
        if (t.getVehicleId() == null) {
            return "";
        }
        Vehicle v = vehicleMapper.selectById(t.getVehicleId());
        return v == null || v.getMileage() == null ? "" : String.valueOf(v.getMileage());
    }

    public List<NotifyMessage> messages(Long id) {
        scoped(id);
        return notifyService.forTask(id);
    }

    /** 仅 PENDING/CANCELLED 任务可删除。 */
    @Transactional
    public void delete(Long id) {
        FollowTask t = scoped(id);
        if (!"PENDING".equals(t.getStatus()) && !"CANCELLED".equals(t.getStatus())) {
            throw new BizException("已完成任务不可删除");
        }
        mapper.deleteById(id);
    }
}
