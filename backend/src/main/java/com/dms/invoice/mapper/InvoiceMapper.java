package com.dms.invoice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.invoice.entity.Invoice;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface InvoiceMapper extends BaseMapper<Invoice> {
    /** 原子占用开具中状态：DRAFT/FAILED -> ISSUING，返回 0 表示被并发抢占。 */
    @Update(
            "UPDATE dms_invoice SET status = 'ISSUING', updated_at = NOW()"
                    + " WHERE id = #{id} AND status IN ('DRAFT','FAILED')")
    int markIssuing(@Param("id") Long id);

    /** 原子占用红冲中状态：ISSUED -> RED_FLUSHING。 */
    @Update(
            "UPDATE dms_invoice SET status = 'RED_FLUSHING', updated_at = NOW()"
                    + " WHERE id = #{id} AND status = 'ISSUED'")
    int markRedFlushing(@Param("id") Long id);
}
