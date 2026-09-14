package com.dms.survey.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_survey_question")
public class SurveyQuestion extends BaseEntity {
    private Long templateId;
    private Integer seq;
    private String text;
    private String type; // SCORE/NPS/TEXT
    private BigDecimal weight;
}
