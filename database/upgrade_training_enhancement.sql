-- ============================================================
-- 实训模块完善 — 数据库升级脚本
-- 功能：
--   1. training_task 新增 task_type 字段（区分 TASK / DOCUMENT）
--   2. 新建 training_material 表（教师上传资料）
--   3. 现有 attachment_file 表直接支持 PLAN 类型（无需改表结构）
-- ============================================================

-- 1. training_task 新增 task_type 字段
ALTER TABLE `training_task`
  ADD COLUMN `task_type` VARCHAR(20) DEFAULT 'TASK' COMMENT '任务类型：TASK(普通任务) / DOCUMENT(文档提交)'
  AFTER `sort_order`;

-- 2. 新建 training_material 实训资料表
DROP TABLE IF EXISTS `training_material`;
CREATE TABLE `training_material` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `project_id`  BIGINT       NOT NULL COMMENT '实训项目ID',
  `title`       VARCHAR(200) NOT NULL COMMENT '资料标题',
  `file_url`    VARCHAR(500) NOT NULL COMMENT '文件访问URL',
  `file_name`   VARCHAR(200) DEFAULT NULL COMMENT '原始文件名',
  `file_size`   BIGINT       DEFAULT 0 COMMENT '文件大小（字节）',
  `create_by`   BIGINT       DEFAULT NULL COMMENT '上传教师ID',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实训资料表';
