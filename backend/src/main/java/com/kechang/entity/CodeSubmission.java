package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 代码提交记录实体
 */
@Data
@TableName("code_submission")
public class CodeSubmission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private Long questionId;
    private Long examId;
    private Long recordId;
    private String language;
    private String sourceCode;
    private String resultStatus;    // AC/WA/TLE/MLE/RE/CE
    private Integer passedCases;
    private Integer totalCases;
    private String compileError;
    private Integer executionTime;
    private Integer executionMemory;
    private String caseDetails;     // JSON

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
