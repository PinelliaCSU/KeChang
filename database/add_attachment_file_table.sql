-- ============================================================
-- 附件文件表（支持作业/提交的多文件上传）
-- 运行方式：在项目数据库中执行此脚本
-- ============================================================

DROP TABLE IF EXISTS `attachment_file`;

CREATE TABLE `attachment_file` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `target_type` VARCHAR(20)  NOT NULL COMMENT '目标类型：ASSIGNMENT(作业附件) / SUBMIT(提交附件)',
  `target_id`   BIGINT       NOT NULL COMMENT '目标ID（作业ID或提交ID）',
  `file_name`   VARCHAR(200) NOT NULL COMMENT '原始文件名',
  `file_url`    VARCHAR(500) NOT NULL COMMENT '文件访问URL',
  `file_size`   BIGINT       DEFAULT 0 COMMENT '文件大小（字节）',
  `sort_order`  INT          DEFAULT 0 COMMENT '排序号',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='附件文件表';
