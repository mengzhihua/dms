package com.dms.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.auth.DataScope;
import com.dms.common.BizException;
import com.dms.crm.entity.NotifyMessage;
import com.dms.crm.entity.NotifyTemplate;
import com.dms.crm.mapper.NotifyMessageMapper;
import com.dms.crm.mapper.NotifyTemplateMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 通知发送：模板渲染 → 落库 PENDING → 通道发送 → SENT/FAILED。 */
@Service
@RequiredArgsConstructor
public class NotifyService {
    private final NotifyMessageMapper mapper;
    private final NotifyTemplateMapper templateMapper;
    private final NotifyChannel smsChannel;
    private final NotifyChannel wechatChannel;

    public NotifyMessage send(
            String dealerCode,
            String channel,
            String receiver,
            String templateCode,
            Map<String, String> vars,
            String bizType,
            Long bizId) {
        if (receiver == null || receiver.trim().isEmpty()) {
            throw new BizException("客户无手机号");
        }
        NotifyTemplate tpl =
                templateMapper.selectOne(
                        new QueryWrapper<NotifyTemplate>().eq("code", templateCode));
        if (tpl == null || !Boolean.TRUE.equals(tpl.getEnabled())) {
            throw new BizException("通知模板不存在或未启用: " + templateCode);
        }
        NotifyMessage m = new NotifyMessage();
        m.setDealerCode(dealerCode);
        m.setChannel(channel);
        m.setReceiver(receiver);
        m.setTemplateCode(templateCode);
        m.setContent(render(tpl.getContent(), vars));
        m.setBizType(bizType);
        m.setBizId(bizId);
        m.setStatus("PENDING");
        mapper.insert(m);
        return dispatch(m);
    }

    /** 模板存在且启用。 */
    public boolean hasTemplate(String code) {
        Long cnt =
                templateMapper.selectCount(
                        new QueryWrapper<NotifyTemplate>()
                                .eq("code", code)
                                .eq("enabled", true));
        return cnt != null && cnt > 0;
    }

    /** FAILED 消息重发：条件 UPDATE 抢占，防止并发重发。 */
    public NotifyMessage retry(Long id) {
        NotifyMessage m = mustGet(id);
        DataScope.check(m.getDealerCode());
        if (mapper.claimRetry(id, LocalDateTime.now().minusMinutes(10)) == 0) {
            throw new BizException("仅失败消息可重发");
        }
        m.setStatus("PENDING");
        try {
            return dispatch(m);
        } catch (RuntimeException e) {
            // 发送过程异常：落回 FAILED，避免消息永远停在 PENDING
            m.setStatus("FAILED");
            m.setErrorMsg(e.getMessage());
            mapper.updateById(m);
            throw new BizException("重发失败: " + e.getMessage());
        }
    }

    private NotifyMessage dispatch(NotifyMessage m) {
        NotifyChannel ch = "WECHAT".equals(m.getChannel()) ? wechatChannel : smsChannel;
        NotifyChannel.SendResult r = ch.send(m);
        if (r.success) {
            m.setStatus("SENT");
            m.setProviderRef(r.providerRef);
            m.setErrorMsg(null);
            m.setSentAt(LocalDateTime.now());
        } else {
            m.setStatus("FAILED");
            m.setErrorMsg(r.errorMsg);
        }
        mapper.updateById(m);
        return m;
    }

    private NotifyMessage mustGet(Long id) {
        NotifyMessage m = mapper.selectById(id);
        if (m == null) {
            throw new BizException("通知消息不存在");
        }
        return m;
    }

    public List<NotifyMessage> forTask(Long taskId) {
        return mapper.selectList(
                new QueryWrapper<NotifyMessage>()
                        .eq("biz_type", "FOLLOW_TASK")
                        .eq("biz_id", taskId)
                        .orderByDesc("id"));
    }

    /** 模板渲染：{{key}} 占位符替换。 */
    static String render(String content, Map<String, String> vars) {
        if (content == null) {
            return "";
        }
        String out = content;
        if (vars != null) {
            for (Map.Entry<String, String> e : vars.entrySet()) {
                out = out.replace("{{" + e.getKey() + "}}", e.getValue() == null ? "" : e.getValue());
            }
        }
        return out;
    }
}
