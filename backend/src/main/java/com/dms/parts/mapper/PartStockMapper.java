package com.dms.parts.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.parts.entity.PartStock;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface PartStockMapper extends BaseMapper<PartStock> {
    /** 原子入库累加 */
    @Update(
            "UPDATE dms_part_stock SET qty = qty + #{n}, updated_at = NOW() WHERE id = #{id}")
    int atomicAddQty(@Param("id") Long id, @Param("n") int n);

    /** 原子预留：仅当可用量(qty-reserved)足够时成功 */
    @Update(
            "UPDATE dms_part_stock SET reserved_qty = reserved_qty + #{need}, updated_at = NOW()"
                    + " WHERE id = #{id} AND qty - reserved_qty >= #{need}")
    int atomicReserve(@Param("id") Long id, @Param("need") int need);

    @Update(
            "UPDATE dms_part_stock SET reserved_qty = reserved_qty - #{n}, updated_at = NOW()"
                    + " WHERE id = #{id} AND reserved_qty >= #{n}")
    int atomicRelease(@Param("id") Long id, @Param("n") int n);

    /** 原子消耗：预留转出库 */
    @Update(
            "UPDATE dms_part_stock SET qty = qty - #{n}, reserved_qty = reserved_qty - #{n},"
                    + " updated_at = NOW() WHERE id = #{id} AND reserved_qty >= #{n} AND qty >= #{n}")
    int atomicConsume(@Param("id") Long id, @Param("n") int n);
}
