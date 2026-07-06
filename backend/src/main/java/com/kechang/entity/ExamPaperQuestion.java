package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 试卷-试题关联实体
 */
@Data
@TableName("exam_paper_question")
public class ExamPaperQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long paperId;
    private Long questionId;
    private Double questionScore;
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
