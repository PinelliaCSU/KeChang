-- ============================================================
-- 课堂签到系统 数据库迁移脚本
-- 表：checkin_session（签到场次）、checkin_record（签到记录）
-- 日期：2026-07-10
-- ============================================================

USE kechang;

-- ============================================================
-- 签到场次表
-- ============================================================
DROP TABLE IF EXISTS `checkin_session`;
CREATE TABLE `checkin_session` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `course_id`       BIGINT        NOT NULL COMMENT '课程ID',
  `title`           VARCHAR(100)  DEFAULT '课堂签到' COMMENT '签到标题',
  `checkin_type`    VARCHAR(20)   NOT NULL DEFAULT 'NUMERIC_CODE' COMMENT '签到方式：QR_CODE / NUMERIC_CODE / LOCATION',
  `code`            VARCHAR(10)   DEFAULT NULL COMMENT '6位数字签到码（NUMERIC_CODE和QR_CODE共用）',
  `latitude`        DECIMAL(10,7) DEFAULT NULL COMMENT '地理位置签到纬度（LOCATION方式）',
  `longitude`       DECIMAL(10,7) DEFAULT NULL COMMENT '地理位置签到经度（LOCATION方式）',
  `location_radius` INT           DEFAULT 100 COMMENT '有效半径（米），默认100',
  `start_time`      DATETIME      NOT NULL COMMENT '签到开始时间',
  `end_time`        DATETIME      NOT NULL COMMENT '签到截止时间',
  `status`          VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE / ENDED',
  `create_by`       BIGINT        NOT NULL COMMENT '发起教师ID',
  `create_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT(1)    DEFAULT 0 COMMENT '逻辑删除：0正常 / 1已删除',
  INDEX `idx_course_id` (`course_id`),
  INDEX `idx_create_by` (`create_by`),
  INDEX `idx_status` (`status`),
  INDEX `idx_code` (`code`),
  INDEX `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签到场次表';

-- ============================================================
-- 签到记录表
-- ============================================================
DROP TABLE IF EXISTS `checkin_record`;
CREATE TABLE `checkin_record` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `session_id`      BIGINT        NOT NULL COMMENT '关联签到场次ID',
  `student_id`      BIGINT        NOT NULL COMMENT '学生用户ID',
  `checkin_time`    DATETIME      NOT NULL COMMENT '签到时间',
  `checkin_method`  VARCHAR(20)   DEFAULT NULL COMMENT '实际使用方式（同session的checkin_type）',
  `latitude`        DECIMAL(10,7) DEFAULT NULL COMMENT '学生签到时的纬度',
  `longitude`       DECIMAL(10,7) DEFAULT NULL COMMENT '学生签到时的经度',
  `status`          VARCHAR(10)   NOT NULL DEFAULT 'SUCCESS' COMMENT '签到状态：SUCCESS / LATE / ABSENT',
  `create_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted`      TINYINT(1)    DEFAULT 0 COMMENT '逻辑删除：0正常 / 1已删除',
  UNIQUE INDEX `uk_session_student` (`session_id`, `student_id`),
  INDEX `idx_student_id` (`student_id`),
  INDEX `idx_session_id` (`session_id`),
  INDEX `idx_checkin_time` (`checkin_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签到记录表';
