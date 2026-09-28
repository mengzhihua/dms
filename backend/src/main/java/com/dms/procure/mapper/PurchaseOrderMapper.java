package com.dms.procure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.procure.entity.PurchaseOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {

    /** 并发安全挂账：仅未归属对账单且已到货/已关闭的采购单可被认领，返回 0 表示已被并发占用。 */
    @Update(
            "UPDATE dms_purchase_order SET statement_id=#{sid},"
                    + " updated_at=NOW() WHERE id=#{id} AND statement_id IS NULL"
                    + " AND status IN ('RECEIVED','CLOSED')")
    int attachToStatement(@Param("sid") Long statementId, @Param("id") Long orderId);
}
