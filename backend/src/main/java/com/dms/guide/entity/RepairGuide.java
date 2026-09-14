package com.dms.guide.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_repair_guide")
public class RepairGuide extends BaseEntity {
    private String code;
    private String title;
    private String modelCodes;
    private String dtcCodes;
    private String symptoms;
    private String diagnosisSteps;
    private String repairSteps;
    private String laborItemCodes;
    private String partNos;
    private String difficulty;
    private String safetyNotes;
}
