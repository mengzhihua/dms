package com.dms.survey.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_survey_template")
public class SurveyTemplate extends BaseEntity {
    private String code;
    private String name;
    private String type; // SERVICE/SALES
}
