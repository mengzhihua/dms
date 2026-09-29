package com.dms.crm;

import static org.junit.jupiter.api.Assertions.*;

import com.dms.DmsApplication;
import com.dms.crm.entity.FollowTask;
import com.dms.crm.mapper.FollowTaskMapper;
import com.dms.customer.entity.Vehicle;
import com.dms.crm.entity.NotifyMessage;
import com.dms.crm.mapper.NotifyMessageMapper;
import com.dms.customer.entity.Vehicle;
import com.dms.customer.mapper.VehicleMapper;
import com.dms.workshop.entity.WorkOrder;
import com.dms.workshop.mapper.WorkOrderMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** 客户关系全流程：自动生成任务 → 完成/通知 → 幂等与越权校验。 */
@SpringBootTest(
        classes = DmsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:crmflow;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1"
        })
class CrmFlowTest {
    @Autowired TestRestTemplate http;
    @Autowired FollowTaskMapper taskMapper;
    @Autowired VehicleMapper vehicleMapper;
    @Autowired NotifyMessageMapper msgMapper;
    @Autowired com.dms.crm.service.NotifyService notifyService;
    @Autowired WorkOrderMapper orderMapper;
    @Autowired com.dms.workshop.service.WorkOrderService orderService;
    @Autowired com.dms.crm.service.FollowTaskGenerator generator;

    private String login(String username) {
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", "123456");
        ResponseEntity<Map> r = http.postForEntity("/api/auth/login", body, Map.class);
        return (String) ((Map<String, Object>) r.getBody().get("data")).get("token");
    }

    private HttpEntity<?> auth(String token, Object body) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return body == null ? new HttpEntity<>(h) : new HttpEntity<>(body, h);
    }

    @SuppressWarnings("unchecked")
    private <T> T call(String token, String method, String path, Object body) {
        ResponseEntity<Map> r =
                http.exchange(path, HttpMethod.valueOf(method), auth(token, body), Map.class);
        assertEquals(0, ((Number) r.getBody().get("code")).intValue(), path + " -> " + r.getBody());
        return (T) r.getBody().get("data");
    }

    private ResponseEntity<Map> raw(String token, String method, String path, Object body) {
        return http.exchange(path, HttpMethod.valueOf(method), auth(token, body), Map.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void generateCompleteNotifyFlow() {
        String sa = login("d001sa");

        // 种子车辆保养早已到期 → 生成 MAINTENANCE_REMIND
        Map<String, Object> gen = new HashMap<>();
        gen.put("dealerCode", "D001");
        Object count = call(sa, "POST", "/api/crm/task/generate", gen);
        assertTrue(((Number) count).intValue() >= 1, "generate -> " + count);

        List<Map<String, Object>> list =
                call(sa, "GET", "/api/crm/task/list?type=MAINTENANCE_REMIND", null);
        assertFalse(list.isEmpty());
        Long taskId = ((Number) list.get(0).get("id")).longValue();

        // 幂等：再次生成数量不变
        Object count2 = call(sa, "POST", "/api/crm/task/generate", gen);
        assertEquals(0, ((Number) count2).intValue());

        // 通知：MOCK 通道 SENT，内容渲染客户姓名
        Map<String, Object> n = new HashMap<>();
        n.put("channel", "SMS");
        Map<String, Object> msg = call(sa, "POST", "/api/crm/task/" + taskId + "/notify", n);
        assertEquals("SENT", msg.get("status"));
        assertTrue(
                String.valueOf(msg.get("content")).contains("沪A10001")
                        && !String.valueOf(msg.get("content")).contains("{{"),
                String.valueOf(msg));

        // 微信渠道优先使用 MAINT_REMIND_WX 模板
        Map<String, Object> nw = new HashMap<>();
        nw.put("channel", "WECHAT");
        Map<String, Object> wmsg = call(sa, "POST", "/api/crm/task/" + taskId + "/notify", nw);
        assertEquals("MAINT_REMIND_WX", wmsg.get("templateCode"));
        assertTrue(String.valueOf(wmsg.get("content")).contains("预约"), String.valueOf(wmsg));

        List<Map<String, Object>> msgs =
                call(sa, "GET", "/api/crm/task/" + taskId + "/messages", null);
        assertFalse(msgs.isEmpty());

        // 完成
        Map<String, Object> done = new HashMap<>();
        done.put("result", "已电话回访，客户满意");
        Map<String, Object> t = call(sa, "POST", "/api/crm/task/" + taskId + "/complete", done);
        assertEquals("DONE", t.get("status"));

        // DONE 任务不能再完成/删除
        assertNotEquals(
                0,
                raw(sa, "POST", "/api/crm/task/" + taskId + "/complete", done)
                        .getBody()
                        .get("code"));
        assertNotEquals(
                0, raw(sa, "DELETE", "/api/crm/task/" + taskId, null).getBody().get("code"));
    }

    @Test
    void deliverCreatesServiceFollowup() {
        // 直接落库一张 DELIVERED 工单，触发生成；再模拟交车钩子路径走批量生成同效果
        WorkOrder o = new WorkOrder();
        o.setOrderNo("WO-CRM-T1");
        o.setDealerCode("D001");
        o.setCustomerId(1L);
        o.setStatus("DELIVERED");
        o.setDeliverTime(LocalDateTime.now());
        orderMapper.insert(o);

        String sa = login("d001sa");
        Map<String, Object> gen = new HashMap<>();
        gen.put("dealerCode", "D001");
        call(sa, "POST", "/api/crm/task/generate", gen);
        Long cnt =
                taskMapper.selectCount(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FollowTask>()
                                .eq("type", "SERVICE_FOLLOWUP")
                                .eq("source_ref", "WO-CRM-T1"));
        assertEquals(1L, cnt.longValue());

        // 重复生成不重复插入
        call(sa, "POST", "/api/crm/task/generate", gen);
        cnt =
                taskMapper.selectCount(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FollowTask>()
                                .eq("type", "SERVICE_FOLLOWUP")
                                .eq("source_ref", "WO-CRM-T1"));
        assertEquals(1L, cnt.longValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void scopeAndGuards() {
        String sa = login("d001sa");
        String d002 = login("d002mgr");

        // 手工任务（无手机号 → 通知失败）
        Map<String, Object> b = new HashMap<>();
        b.put("dealerCode", "D001");
        b.put("title", "手工回访");
        b.put("content", "客户张先生咨询保养套餐");
        b.put("dueDate", LocalDate.now().toString());
        Map<String, Object> t = call(sa, "POST", "/api/crm/task", b);
        Long tid = ((Number) t.get("id")).longValue();
        assertEquals("PENDING", t.get("status"));
        assertEquals("MANUAL", t.get("source"));

        // 无手机号通知报错
        Map<String, Object> n = new HashMap<>();
        n.put("channel", "SMS");
        ResponseEntity<Map> r = raw(sa, "POST", "/api/crm/task/" + tid + "/notify", n);
        assertNotEquals(0, r.getBody().get("code"));

        // 原始 PUT 实体禁止
        Map<String, Object> upd = new HashMap<>();
        upd.put("title", "篡改");
        assertNotEquals(
                0, raw(sa, "PUT", "/api/crm/task/" + tid, upd).getBody().get("code"));

        // 跨经销商读取被拒（d002mgr 看不到 D001 任务）
        List<Map<String, Object>> l2 = call(d002, "GET", "/api/crm/task/list", null);
        assertTrue(l2.stream().noneMatch(m -> "D001".equals(m.get("dealerCode"))));
        assertNotEquals(
                0,
                raw(d002, "GET", "/api/crm/task/" + tid + "/messages", null)
                        .getBody()
                        .get("code"));

        // 取消后可删除
        call(sa, "POST", "/api/crm/task/" + tid + "/cancel", null);
        call(sa, "DELETE", "/api/crm/task/" + tid, null);
    }

    @Test
    @SuppressWarnings("unchecked")
    void crossDealerCustomerRejected() {
        String d002 = login("d002mgr");
        String sa = login("d001sa");
        // 取一个 D002 客户 id
        List<Map<String, Object>> custs = call(d002, "GET", "/api/customer/customer/list", null);
        assertFalse(custs.isEmpty());
        Long d002CustomerId = ((Number) custs.get(0).get("id")).longValue();

        Map<String, Object> b = new HashMap<>();
        b.put("dealerCode", "D001");
        b.put("title", "跨店任务");
        b.put("customerId", d002CustomerId);
        ResponseEntity<Map> r = raw(sa, "POST", "/api/crm/task", b);
        assertNotEquals(0, r.getBody().get("code"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void mileageThenDateReminderDedupe() {
        String sa = login("d001sa");
        // 造一辆仅里程触发（下次保养里程-里程<=500 且按日期未到期）的车辆
        Vehicle v = new Vehicle();
        v.setVin("VDEDUP" + System.currentTimeMillis());
        v.setPlateNo("沪DD001");
        v.setDealerCode("D001");
        v.setMileage(19500);
        v.setNextServiceMileage(19800);
        v.setLastServiceDate(java.time.LocalDate.now().plusYears(1)); // 日期分支不触发
        vehicleMapper.insert(v);

        Map<String, Object> gen = new HashMap<>();
        gen.put("dealerCode", "D001");
        call(sa, "POST", "/api/crm/task/generate", gen);
        Long c1 = countPending(v.getVin(), sa);

        // 再让日期窗口也命中：同一周期不得生成第二个 PENDING 提醒
        v.setLastServiceDate(java.time.LocalDate.now().minusMonths(8));
        vehicleMapper.updateById(v);
        call(sa, "POST", "/api/crm/task/generate", gen);
        Long c2 = countPending(v.getVin(), sa);
        assertEquals(1L, c1.longValue());
        assertEquals(1L, c2.longValue(), "同一车辆保养周期只保留一个 PENDING 提醒");
    }

    @Test
    void deliverClosesPendingMaintenanceReminder() {
        String sa = login("d001sa");
        // 车辆 + 一条 PENDING 保养提醒（模拟上一周期生成）
        Vehicle v = new Vehicle();
        v.setVin("VREM" + System.currentTimeMillis());
        v.setPlateNo("沪DD002");
        v.setDealerCode("D001");
        v.setMileage(20000);
        v.setNextServiceMileage(30000);
        v.setLastServiceDate(LocalDate.now().plusYears(1));
        vehicleMapper.insert(v);

        FollowTask t = new FollowTask();
        t.setTaskNo("T-REM-" + System.currentTimeMillis());
        t.setDealerCode("D001");
        t.setVin(v.getVin());
        t.setType("MAINTENANCE_REMIND");
        t.setSource("AUTO");
        t.setSourceRef(v.getVin() + ":KM30000");
        t.setTitle("保养提醒");
        t.setStatus("PENDING");
        taskMapper.insert(t);
        assertEquals(1L, countPending(v.getVin(), sa).longValue());

        // 工单 SETTLED → 交车（deliver 内部调用 createForOrder）
        WorkOrder o = new WorkOrder();
        o.setOrderNo("WO-REM-T" + System.currentTimeMillis());
        o.setDealerCode("D001");
        o.setVehicleId(v.getId());
        o.setVin(v.getVin());
        o.setCustomerId(1L);
        o.setStatus("SETTLED");
        orderMapper.insert(o);
        orderService.deliver(o.getId(), "tester");
        assertEquals(0L, countPending(v.getVin(), sa).longValue());
        assertEquals(
                "DONE",
                taskMapper.selectById(t.getId()).getStatus());
        assertNotNull(taskMapper.selectById(t.getId()).getDoneAt());

        // 新周期可重新生成
        v.setLastServiceDate(LocalDate.now().minusMonths(8));
        vehicleMapper.updateById(v);
        Map<String, Object> gen = new HashMap<>();
        gen.put("dealerCode", "D001");
        call(sa, "POST", "/api/crm/task/generate", gen);
        assertEquals(1L, countPending(v.getVin(), sa).longValue());
        // 清理本次 generate 产生的全部 PENDING 提醒（含本测试车辆），
        // 让后续用例的 generate 重新为种子车辆建任务且 notify 选中的任务有客户手机号
        taskMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FollowTask>()
                        .eq("type", "MAINTENANCE_REMIND")
                        .eq("status", "PENDING"));
        // 本测试手工插入的提醒（已置 DONE）也要删除，避免抢占 list.get(0) 影响其他用例
        taskMapper.deleteById(t.getId());
    }

    @Test
    void deliverDefaultsNextServiceMileage() {
        // 车型无保养间隔（甚至无车型）时，交车按默认 10000km 推进下次保养里程
        Vehicle v = new Vehicle();
        v.setVin("VDEF" + System.currentTimeMillis());
        v.setPlateNo("沪DD003");
        v.setDealerCode("D001");
        v.setMileage(42000);
        v.setNextServiceMileage(50000);
        vehicleMapper.insert(v);

        WorkOrder o = new WorkOrder();
        o.setOrderNo("WO-DEF-T" + System.currentTimeMillis());
        o.setDealerCode("D001");
        o.setVehicleId(v.getId());
        o.setVin(v.getVin());
        o.setCustomerId(1L);
        o.setStatus("SETTLED");
        orderMapper.insert(o);
        orderService.deliver(o.getId(), "tester");

        assertEquals(52000, vehicleMapper.selectById(v.getId()).getNextServiceMileage().intValue());
    }

    private Long countPending(String vin, String token) {
        List<Map<String, Object>> list =
                call(token, "GET", "/api/crm/task/list?type=MAINTENANCE_REMIND&status=PENDING", null);
        return list.stream().filter(t -> vin.equals(t.get("vin"))).count();
    }

    @Test
    @SuppressWarnings("unchecked")
    void adminCrossDealerReferenceRejected() {
        String admin = login("admin");
        String d002 = login("d002mgr");
        List<Map<String, Object>> custs = call(d002, "GET", "/api/customer/customer/list", null);
        assertFalse(custs.isEmpty());
        Long d002CustomerId = ((Number) custs.get(0).get("id")).longValue();

        // ADMIN 为网络范围角色：D001 任务不得引用 D002 客户
        Map<String, Object> b = new HashMap<>();
        b.put("dealerCode", "D001");
        b.put("title", "跨店引用");
        b.put("customerId", d002CustomerId);
        ResponseEntity<Map> r = raw(admin, "POST", "/api/crm/task", b);
        assertNotEquals(0, r.getBody().get("code"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void retryFailedMessageAndChannelThrow() {
        String sa = login("d001sa");
        // 触发一条通知消息
        Map<String, Object> gen = new HashMap<>();
        gen.put("dealerCode", "D001");
        call(sa, "POST", "/api/crm/task/generate", gen);
        List<Map<String, Object>> list =
                call(sa, "GET", "/api/crm/task/list?type=MAINTENANCE_REMIND&status=PENDING", null);
        assertFalse(list.isEmpty());
        Long taskId = ((Number) list.get(0).get("id")).longValue();
        Map<String, Object> n = new HashMap<>();
        n.put("channel", "SMS");
        Map<String, Object> msg = call(sa, "POST", "/api/crm/task/" + taskId + "/notify", n);
        Long mid = ((Number) msg.get("id")).longValue();
        // SENT 状态不可重发
        assertNotEquals(
                0, raw(sa, "POST", "/api/crm/message/" + mid + "/retry", null).getBody().get("code"));
        // 手工置 FAILED 后可重发（mock 通道再次成功）
        NotifyMessage m = msgMapper.selectById(mid);
        m.setStatus("FAILED");
        msgMapper.updateById(m);
        Map<String, Object> re = call(sa, "POST", "/api/crm/message/" + mid + "/retry", null);
        assertEquals("SENT", re.get("status"));

        // 通道抛异常：消息落回 FAILED 而不是卡在 PENDING
        Object orig =
                org.springframework.test.util.ReflectionTestUtils.getField(
                        notifyService, "smsChannel");
        org.springframework.test.util.ReflectionTestUtils.setField(
                notifyService,
                "smsChannel",
                new com.dms.crm.service.NotifyChannel() {
                            @Override
                            public String channel() {
                                return "SMS";
                            }

                            @Override
                            public SendResult send(NotifyMessage mm) {
                                throw new RuntimeException("mock channel boom");
                            }
                        });
        try {
            m.setStatus("FAILED");
            m.setErrorMsg("x");
            msgMapper.updateById(m);
            assertNotEquals(
                    0,
                    raw(sa, "POST", "/api/crm/message/" + mid + "/retry", null)
                            .getBody()
                            .get("code"));
            assertEquals("FAILED", msgMapper.selectById(mid).getStatus());
        } finally {
            org.springframework.test.util.ReflectionTestUtils.setField(
                    notifyService, "smsChannel", orig);
        }
    }
}