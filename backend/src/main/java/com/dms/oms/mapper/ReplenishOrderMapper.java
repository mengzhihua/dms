package com.dms.oms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.oms.entity.ReplenishOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ReplenishOrderMapper extends BaseMapper<ReplenishOrder> {
    /** 条件状态迁移,返回 0 表示被并发抢占 */
    @Update("UPDATE dms_replenish_order SET status = #{to}, updated_at = NOW() "
            + "WHERE id = #{id} AND status = #{from}")
    int transit(@Param("id") Long id, @Param("from") String from, @Param("to") String to);
}
