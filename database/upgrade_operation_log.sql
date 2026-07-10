-- ============================================================
-- 操作日志 / 管理端审计
-- ============================================================

CREATE TABLE IF NOT EXISTS `operation_log` (
  `id`            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `operator_id`   BIGINT       DEFAULT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(100) DEFAULT NULL COMMENT '操作人姓名',
  `role_code`     VARCHAR(50)  DEFAULT NULL COMMENT '角色编码',
  `module`        VARCHAR(50)  NOT NULL COMMENT '业务模块',
  `action`        VARCHAR(50)  NOT NULL COMMENT '操作动作',
  `target_type`   VARCHAR(50)  DEFAULT NULL COMMENT '目标类型',
  `target_id`     BIGINT       DEFAULT NULL COMMENT '目标ID',
  `target_name`   VARCHAR(255) DEFAULT NULL COMMENT '目标名称',
  `description`   VARCHAR(500) DEFAULT NULL COMMENT '操作描述',
  `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `is_deleted`    TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_operator_id` (`operator_id`),
  INDEX `idx_module` (`module`),
  INDEX `idx_action` (`action`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台操作日志表';
