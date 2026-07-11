-- ============================================================
-- 视频资源批注持久化
-- 说明：视频文件仍保存在 uploads / 对象存储，批注这种结构化业务数据保存到数据库。
-- ============================================================

CREATE TABLE IF NOT EXISTS `video_annotation` (
  `id`            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `user_id`       BIGINT        NOT NULL COMMENT '批注用户ID',
  `file_key_hash` CHAR(64)      NOT NULL COMMENT '文件标识SHA-256，用于查询',
  `file_key`      VARCHAR(1000) NOT NULL COMMENT '原始文件标识/URL',
  `time_seconds`  DECIMAL(10,2) DEFAULT 0 COMMENT '视频时间点，单位秒',
  `content`       TEXT          NOT NULL COMMENT '批注内容',
  `author`        VARCHAR(100)  DEFAULT NULL COMMENT '显示作者',
  `create_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)    DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_user_file` (`user_id`, `file_key_hash`),
  INDEX `idx_time` (`time_seconds`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频资源批注表';
