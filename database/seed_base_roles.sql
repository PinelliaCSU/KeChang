-- =============================================================
-- 课畅 - 基础角色初始化
-- Docker 新库初始化、手动补库均可重复执行
-- =============================================================

INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `is_deleted`)
VALUES
    (1, 'ROLE_ADMIN', '管理员', 0),
    (2, 'ROLE_TEACHER', '教师', 0),
    (3, 'ROLE_STUDENT', '学生', 0)
ON DUPLICATE KEY UPDATE
    `role_code` = VALUES(`role_code`),
    `role_name` = VALUES(`role_name`),
    `is_deleted` = 0;
