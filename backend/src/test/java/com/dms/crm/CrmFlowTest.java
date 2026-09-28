package com.dms.crm;

import static org.junit.jupiter.api.Assertions.*;

import com.dms.DmsApplication;
import com.dms.crm.entity.FollowTask;
import com.dms.crm.mapper.FollowTaskMapper;
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
    @Autowired WorkOrderMapper orderMapper;

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
}
