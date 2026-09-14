package com.dms.survey.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_survey_answer")
public class SurveyAnswer extends BaseEntity {
    private Long surveyId;
    private Long questionId;
    private BigDecimal score;
    private String text;
}
