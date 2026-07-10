-- ============================================================
-- 随机点名 / 课堂积分
-- ============================================================

CREATE TABLE IF NOT EXISTS `participation_point` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `course_id`   BIGINT      NOT NULL COMMENT '课程ID',
  `student_id`  BIGINT      NOT NULL COMMENT '学生用户ID',
  `teacher_id`  BIGINT      DEFAULT NULL COMMENT '操作教师ID',
  `points`      INT         DEFAULT 1 COMMENT '积分',
  `source_type` VARCHAR(30) DEFAULT '随机点名' COMMENT '来源类型',
  `reason`      VARCHAR(255) DEFAULT NULL COMMENT '积分说明',
  `event_time`  DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT(1)  DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_student_id` (`student_id`),
  INDEX `idx_course_id` (`course_id`),
  INDEX `idx_event_time` (`event_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课堂参与积分流水表';
