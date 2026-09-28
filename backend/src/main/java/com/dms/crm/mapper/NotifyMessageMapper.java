package com.dms.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.crm.entity.NotifyMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface NotifyMessageMapper extends BaseMapper<NotifyMessage> {

    /** 并发安全抢占重发：仅 FAILED 可重置为 PENDING，返回 0 表示已被抢占或状态不符。 */
    @Update("UPDATE dms_notify_message SET status='PENDING', updated_at=NOW() WHERE id=#{id}"
            + " AND (status='FAILED' OR (status='PENDING' AND updated_at < #{staleBefore}))")
    int claimRetry(@Param("id") Long id, @Param("staleBefore") java.time.LocalDateTime staleBefore);
}
