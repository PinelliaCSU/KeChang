-- =============================================================
-- 课畅 - 在线编程题支持
-- 新增 CODE 题型、编程配置字段、代码提交记录表
-- =============================================================

-- 1. 扩展 question 表，新增编程题配置 JSON 字段
ALTER TABLE question
    ADD COLUMN programming_config JSON DEFAULT NULL
    COMMENT '编程题配置：{language,templateCode,timeLimit,memoryLimit,testCases,hiddenTestCases,scoringMode}';

-- 2. 新增代码提交记录表（记录学生每次编译运行操作）
CREATE TABLE IF NOT EXISTS `code_submission` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id`      BIGINT NOT NULL              COMMENT '学生ID',
    `question_id`     BIGINT NOT NULL              COMMENT '题目ID',
    `exam_id`         BIGINT DEFAULT NULL          COMMENT '考试ID（考试内提交时有值）',
    `record_id`       BIGINT DEFAULT NULL          COMMENT '考试记录ID',
    `language`        VARCHAR(20) NOT NULL         COMMENT '编程语言：c/cpp/python',
    `source_code`     TEXT NOT NULL                COMMENT '提交的源代码',
    `result_status`   VARCHAR(20) DEFAULT NULL     COMMENT '判题结果：AC/WA/TLE/MLE/RE/CE',
    `passed_cases`    INT DEFAULT 0                COMMENT '通过用例数',
    `total_cases`     INT DEFAULT 0                COMMENT '总用例数',
    `compile_error`   TEXT DEFAULT NULL            COMMENT '编译错误信息',
    `execution_time`  INT DEFAULT NULL             COMMENT '最大执行时间(ms)',
    `execution_memory` INT DEFAULT NULL            COMMENT '最大内存使用(KB)',
    `case_details`    JSON DEFAULT NULL            COMMENT '每个用例的详细结果',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_student` (`student_id`),
    INDEX `idx_question` (`question_id`),
    INDEX `idx_record` (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代码提交记录';

-- 3. 更新 exam_record_answer 表说明（无需改表结构，编程题答案以源代码文本存储在 student_answer 字段）
-- student_answer 字段已为 TEXT 类型，足够存储源代码
