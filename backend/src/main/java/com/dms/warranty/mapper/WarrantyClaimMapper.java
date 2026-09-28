package com.dms.warranty.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.warranty.entity.WarrantyClaim;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface WarrantyClaimMapper extends BaseMapper<WarrantyClaim> {

    /** 并发安全挂账：仅 APPROVED 且未归属结算单的索赔单可被批次认领，返回 0 表示已被并发占用。 */
    @Update(
            "UPDATE dms_warranty_claim SET settlement_id=#{sid}, status='SETTLED',"
                    + " updated_at=NOW() WHERE id=#{id} AND settlement_id IS NULL AND status='APPROVED'")
    int attachToSettlement(@Param("sid") Long settlementId, @Param("id") Long claimId);
}
