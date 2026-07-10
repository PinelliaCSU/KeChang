-- ============================================================
-- 随堂测验支持多题
-- 已经执行过 upgrade_class_quiz.sql 的库，再执行本脚本
-- ============================================================

CREATE TABLE IF NOT EXISTS `class_quiz_question` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `quiz_id`     BIGINT      NOT NULL COMMENT '随堂测验ID',
  `question_id` BIGINT      NOT NULL COMMENT '试题ID',
  `sort_order`  INT         DEFAULT 0 COMMENT '题目顺序',
  `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT(1)  DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE INDEX `idx_quiz_question` (`quiz_id`, `question_id`),
  INDEX `idx_quiz_id` (`quiz_id`),
  INDEX `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='随堂测验题目关联表';

ALTER TABLE `class_quiz_answer`
  ADD COLUMN `question_id` BIGINT DEFAULT NULL COMMENT '试题ID' AFTER `quiz_id`;

ALTER TABLE `class_quiz_answer`
  DROP INDEX `idx_quiz_student`;

ALTER TABLE `class_quiz_answer`
  ADD UNIQUE INDEX `idx_quiz_student_question` (`quiz_id`, `student_id`, `question_id`);

INSERT IGNORE INTO `class_quiz_question` (`quiz_id`, `question_id`, `sort_order`)
SELECT `id`, `question_id`, 1
FROM `class_quiz`
WHERE `question_id` IS NOT NULL;

UPDATE `class_quiz_answer` a
JOIN `class_quiz` q ON q.id = a.quiz_id
SET a.question_id = q.question_id
WHERE a.question_id IS NULL;
