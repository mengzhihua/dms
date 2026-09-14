package com.dms.guide.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_technical_bulletin")
public class TechnicalBulletin extends BaseEntity {
    private String code;
    private String title;
    private String modelCodes;
    private String content;
    private LocalDate issueDate;
}
