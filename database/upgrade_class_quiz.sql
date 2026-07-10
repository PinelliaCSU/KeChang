-- ============================================================
-- 随堂测验模块：教师实时出题、学生限时作答、实时统计分布
-- ============================================================

CREATE TABLE IF NOT EXISTS `class_quiz` (
  `id`               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `course_id`        BIGINT       NOT NULL COMMENT '课程ID',
  `question_id`      BIGINT       NOT NULL COMMENT '题库试题ID',
  `title`            VARCHAR(255) DEFAULT NULL COMMENT '测验标题',
  `duration_seconds` INT          DEFAULT 120 COMMENT '限时秒数',
  `status`           VARCHAR(20)  DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE / ENDED',
  `create_by`        BIGINT       DEFAULT NULL COMMENT '创建教师ID',
  `start_time`       DATETIME     NOT NULL COMMENT '开始时间',
  `end_time`         DATETIME     NOT NULL COMMENT '结束时间',
  `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`       TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_course_id` (`course_id`),
  INDEX `idx_question_id` (`question_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='随堂测验会话表';

CREATE TABLE IF NOT EXISTS `class_quiz_answer` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `quiz_id`     BIGINT      NOT NULL COMMENT '随堂测验ID',
  `student_id`  BIGINT      NOT NULL COMMENT '学生用户ID',
  `answer`      TEXT        DEFAULT NULL COMMENT '学生答案',
  `is_correct`  TINYINT(1)  DEFAULT NULL COMMENT '是否正确：0错误 / 1正确',
  `submit_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT(1)  DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE INDEX `idx_quiz_student` (`quiz_id`, `student_id`),
  INDEX `idx_quiz_id` (`quiz_id`),
  INDEX `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='随堂测验作答表';
