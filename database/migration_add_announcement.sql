-- ============================================================
-- 公告模块迁移：扩展现有 notification 表支持课程公告
-- ============================================================

-- 1. 添加 course_id 列（仅公告类型使用）
ALTER TABLE `notification`
ADD COLUMN `course_id` BIGINT DEFAULT NULL COMMENT '关联课程ID(仅公告类型使用)' AFTER `notification_type`,
ADD INDEX `idx_course_id` (`course_id`);

-- 2. 更新 notification_type 注释（新增 '公告' 类型）
ALTER TABLE `notification`
MODIFY COLUMN `notification_type` VARCHAR(20) DEFAULT '系统' COMMENT '通知类型：系统 / 课程 / 考试 / 作业 / 公告';
