package com.dms.warranty.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.warranty.entity.WarrantySettlement;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface WarrantySettlementMapper extends BaseMapper<WarrantySettlement> {

    /** 仅草稿可删；返回 0 表示已被并发确认/付款。 */
    @Delete("DELETE FROM dms_warranty_settlement WHERE id=#{id} AND status='DRAFT'")
    int deleteDraft(@Param("id") Long id);
}
