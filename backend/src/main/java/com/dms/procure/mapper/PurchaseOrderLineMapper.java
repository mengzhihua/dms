package com.dms.procure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.procure.entity.PurchaseOrderLine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface PurchaseOrderLineMapper extends BaseMapper<PurchaseOrderLine> {

    /** 条件累加已收数量，防止并发超收；返回受影响行数。 */
    @Update("UPDATE dms_purchase_order_line SET received_qty=received_qty+#{qty}, updated_at=NOW()"
            + " WHERE id=#{id} AND received_qty+#{qty} <= qty")
    int addReceived(@Param("id") Long id, @Param("qty") int qty);
}
