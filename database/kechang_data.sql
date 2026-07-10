-- ============================================================
-- 课畅（KeChang）— 智能化在线教学支持服务平台
-- 完整数据库初始化脚本（合并所有升级脚本）
-- 字符集: utf8mb4 | 引擎: InnoDB
-- ============================================================

CREATE DATABASE IF NOT EXISTS kechang
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE kechang;

-- ============================================================
-- 模块一：用户与权限
-- ============================================================

-- 角色表
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `role_code`   VARCHAR(50)  NOT NULL COMMENT '角色编码（ROLE_TEACHER / ROLE_STUDENT / ROLE_ADMIN）',
  `role_name`   VARCHAR(50)  NOT NULL COMMENT '角色名称',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除：0正常 / 1已删除',
  UNIQUE INDEX `idx_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 核心用户表
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
  `password`    VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
  `email`       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
  `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `role_id`     BIGINT       DEFAULT NULL COMMENT '角色ID（关联sys_role）',
  `status`      TINYINT(1)   DEFAULT 0 COMMENT '状态：0正常 / 1禁用',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除：0正常 / 1已删除',
  UNIQUE INDEX `idx_username` (`username`),
  UNIQUE INDEX `idx_email` (`email`),
  INDEX `idx_role_id` (`role_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='核心用户表';

-- 教师扩展信息表
DROP TABLE IF EXISTS `teacher_profile`;
CREATE TABLE `teacher_profile` (
  `id`           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `user_id`      BIGINT       NOT NULL COMMENT '关联sys_user.id',
  `real_name`    VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
  `title`        VARCHAR(50)  DEFAULT NULL COMMENT '职称（教授/副教授/讲师等）',
  `department`   VARCHAR(100) DEFAULT NULL COMMENT '所属院系',
  `introduction` TEXT         DEFAULT NULL COMMENT '个人简介',
  `avatar`       VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`   TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师扩展信息表';

-- 学生扩展信息表
DROP TABLE IF EXISTS `student_profile`;
CREATE TABLE `student_profile` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `user_id`     BIGINT       NOT NULL COMMENT '关联sys_user.id',
  `student_no`  VARCHAR(30)  DEFAULT NULL COMMENT '学号',
  `real_name`   VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
  `grade`       VARCHAR(20)  DEFAULT NULL COMMENT '年级（如2024级）',
  `major`       VARCHAR(100) DEFAULT NULL COMMENT '专业',
  `class_name`  VARCHAR(50)  DEFAULT NULL COMMENT '班级',
  `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE INDEX `idx_user_id` (`user_id`),
  UNIQUE INDEX `idx_student_no` (`student_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生扩展信息表';

-- ============================================================
-- 模块二：课程
-- ============================================================

DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
  `id`            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `course_name`   VARCHAR(200) NOT NULL COMMENT '课程名称',
  `cover_img`     VARCHAR(255) DEFAULT NULL COMMENT '封面图片URL',
  `description`   TEXT         DEFAULT NULL COMMENT '课程简介',
  `teacher_id`    BIGINT       NOT NULL COMMENT '授课教师ID（关联sys_user）',
  `start_date`    DATE         DEFAULT NULL COMMENT '开课日期',
  `end_date`      DATE         DEFAULT NULL COMMENT '结课日期',
  `status`        VARCHAR(20)  DEFAULT '进行中' COMMENT '课程状态：进行中 / 已结束',
  `student_count` INT          DEFAULT 0 COMMENT '选课人数',
  `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_teacher_id` (`teacher_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_course_name` (`course_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程主表';

DROP TABLE IF EXISTS `course_chapter`;
CREATE TABLE `course_chapter` (
  `id`           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `course_id`    BIGINT       NOT NULL COMMENT '课程ID',
  `chapter_name` VARCHAR(200) NOT NULL COMMENT '章节名称',
  `sort_order`   INT          DEFAULT 0 COMMENT '排序号',
  `description`  TEXT         DEFAULT NULL COMMENT '章节描述',
  `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`   TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程章节表';

DROP TABLE IF EXISTS `course_resource`;
CREATE TABLE `course_resource` (
  `id`            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `course_id`     BIGINT       NOT NULL COMMENT '课程ID',
  `chapter_id`    BIGINT       DEFAULT NULL COMMENT '所属章节ID',
  `resource_name` VARCHAR(200) NOT NULL COMMENT '资源名称',
  `resource_type` VARCHAR(20)  NOT NULL COMMENT '资源类型：VIDEO / PDF / PPT / LINK',
  `file_url`      VARCHAR(500) DEFAULT NULL COMMENT '文件URL',
  `sort_order`    INT          DEFAULT 0 COMMENT '排序号',
  `file_size`     BIGINT       DEFAULT 0 COMMENT '文件大小（字节）',
  `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_course_id` (`course_id`),
  INDEX `idx_chapter_id` (`chapter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程资源表';

DROP TABLE IF EXISTS `course_student`;
CREATE TABLE `course_student` (
  `id`           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `course_id`    BIGINT      NOT NULL COMMENT '课程ID',
  `student_id`   BIGINT      NOT NULL COMMENT '学生用户ID（关联sys_user）',
  `enrolled_time` DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '选课时间',
  `progress`     DECIMAL(5,2) DEFAULT 0.00 COMMENT '学习进度百分比',
  `create_time`  DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`   TINYINT(1)  DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE INDEX `idx_course_student` (`course_id`, `student_id`),
  INDEX `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='选课关联表';

-- ============================================================
-- 模块三：试题与考试
-- ============================================================

DROP TABLE IF EXISTS `question`;
CREATE TABLE `question` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `question_type`   VARCHAR(20)  NOT NULL COMMENT '题型：SINGLE / MULTI / JUDGE / FILL / ESSAY',
  `content`         TEXT         NOT NULL COMMENT '题目内容',
  `options`         JSON         DEFAULT NULL COMMENT '选项（JSON格式）',
  `answer`          TEXT         DEFAULT NULL COMMENT '正确答案',
  `analysis`        TEXT         DEFAULT NULL COMMENT '解析',
  `score`           DECIMAL(5,2) DEFAULT 0.00 COMMENT '默认分值',
  `difficulty`      INT          DEFAULT 3 COMMENT '难度：1-5',
  `knowledge_point` VARCHAR(200) DEFAULT NULL COMMENT '知识点',
  `course_id`       BIGINT       DEFAULT NULL COMMENT '所属课程ID',
  `create_by`       BIGINT       DEFAULT NULL COMMENT '创建教师ID',
  `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_question_type` (`question_type`),
  INDEX `idx_course_id` (`course_id`),
  INDEX `idx_difficulty` (`difficulty`),
  INDEX `idx_knowledge_point` (`knowledge_point`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试题库表';

DROP TABLE IF EXISTS `exam_paper`;
CREATE TABLE `exam_paper` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `paper_name`  VARCHAR(200) NOT NULL COMMENT '试卷名称',
  `course_id`   BIGINT       DEFAULT NULL COMMENT '所属课程ID',
  `total_score` DECIMAL(6,2) DEFAULT 100.00 COMMENT '试卷总分',
  `duration`    INT          DEFAULT 120 COMMENT '考试时长（分钟）',
  `pass_score`  DECIMAL(6,2) DEFAULT 60.00 COMMENT '及格分数线',
  `status`      VARCHAR(20)  DEFAULT '草稿' COMMENT '状态：草稿 / 已发布',
  `create_by`   BIGINT       DEFAULT NULL COMMENT '创建教师ID',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_course_id` (`course_id`),
  INDEX `idx_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试卷表';

DROP TABLE IF EXISTS `exam_paper_question`;
CREATE TABLE `exam_paper_question` (
  `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `paper_id`       BIGINT       NOT NULL COMMENT '试卷ID',
  `question_id`    BIGINT       NOT NULL COMMENT '试题ID',
  `question_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '该题分值',
  `sort_order`     INT          DEFAULT 0 COMMENT '排序号',
  `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`     TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_paper_id` (`paper_id`),
  INDEX `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试卷-试题关联表';

DROP TABLE IF EXISTS `exam`;
CREATE TABLE `exam` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `exam_name`   VARCHAR(200) NOT NULL COMMENT '考试名称',
  `paper_id`    BIGINT       NOT NULL COMMENT '试卷ID',
  `course_id`   BIGINT       DEFAULT NULL COMMENT '课程ID',
  `start_time`  DATETIME     NOT NULL COMMENT '考试开始时间',
  `end_time`    DATETIME     NOT NULL COMMENT '考试结束时间',
  `duration`    INT          DEFAULT 120 COMMENT '考试时长（分钟）',
  `status`      VARCHAR(20)  DEFAULT '未开始' COMMENT '状态：未开始 / 进行中 / 已结束',
  `allow_retry` TINYINT(1)   DEFAULT 0 COMMENT '是否允许重考',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_paper_id` (`paper_id`),
  INDEX `idx_course_id` (`course_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考试安排表';

DROP TABLE IF EXISTS `exam_record`;
CREATE TABLE `exam_record` (
  `id`           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `exam_id`      BIGINT       NOT NULL COMMENT '考试ID',
  `student_id`   BIGINT       NOT NULL COMMENT '学生用户ID',
  `start_time`   DATETIME     DEFAULT NULL COMMENT '开始作答时间',
  `submit_time`  DATETIME     DEFAULT NULL COMMENT '提交时间',
  `total_score`  DECIMAL(6,2) DEFAULT NULL COMMENT '总分',
  `pass_status`  TINYINT(1)   DEFAULT 0 COMMENT '是否及格：0否 / 1是',
  `is_submitted` TINYINT(1)   DEFAULT 0 COMMENT '是否已提交：0否 / 1是',
  `cheat_count`  INT          DEFAULT 0 COMMENT '切屏次数',
  `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`   TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE INDEX `idx_exam_student` (`exam_id`, `student_id`),
  INDEX `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考试记录表';

DROP TABLE IF EXISTS `exam_record_answer`;
CREATE TABLE `exam_record_answer` (
  `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `record_id`      BIGINT       NOT NULL COMMENT '考试记录ID',
  `question_id`    BIGINT       NOT NULL COMMENT '试题ID',
  `student_answer` TEXT         DEFAULT NULL COMMENT '学生答案',
  `is_correct`     TINYINT(1)   DEFAULT NULL COMMENT '客观题是否正确：0错误 / 1正确',
  `score`          DECIMAL(5,2) DEFAULT NULL COMMENT '得分',
  `marked_by`      BIGINT       DEFAULT NULL COMMENT '批阅教师ID（主观题）',
  `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`     TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_record_id` (`record_id`),
  INDEX `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答题详情表';

-- ============================================================
-- 模块四：作业（合并 upgrade_assignment_chapter 升级脚本）
-- ============================================================

DROP TABLE IF EXISTS `assignment`;
CREATE TABLE `assignment` (
  `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `course_id`      BIGINT        NOT NULL COMMENT '课程ID',
  `chapter_id`     BIGINT        DEFAULT NULL COMMENT '所属章节ID',
  `title`          VARCHAR(200)  NOT NULL COMMENT '作业标题',
  `content`        TEXT          DEFAULT NULL COMMENT '作业内容/要求',
  `attachment_url` VARCHAR(500)  DEFAULT NULL COMMENT '附件URL',
  `deadline`       DATETIME      NOT NULL COMMENT '截止时间',
  `total_score`    DECIMAL(5,2)  DEFAULT 100.00 COMMENT '满分',
  `status`         VARCHAR(20)   DEFAULT '进行中' COMMENT '状态：进行中 / 已截止',
  `create_by`      BIGINT        DEFAULT NULL COMMENT '创建教师ID',
  `create_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`     TINYINT(1)    DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_course_id` (`course_id`),
  INDEX `idx_chapter_id` (`chapter_id`),
  INDEX `idx_deadline` (`deadline`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业表';

DROP TABLE IF EXISTS `assignment_submit`;
CREATE TABLE `assignment_submit` (
  `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `assignment_id`  BIGINT       NOT NULL COMMENT '作业ID',
  `student_id`     BIGINT       NOT NULL COMMENT '学生用户ID',
  `content`        TEXT         DEFAULT NULL COMMENT '提交内容',
  `attachment_url` VARCHAR(500) DEFAULT NULL COMMENT '附件URL',
  `submit_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `score`          DECIMAL(5,2) DEFAULT NULL COMMENT '得分',
  `status`         VARCHAR(20)  DEFAULT '待批改' COMMENT '状态：待批改 / 已批改',
  `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`     TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE INDEX `idx_assignment_student` (`assignment_id`, `student_id`),
  INDEX `idx_student_id` (`student_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业提交表';

-- 作业评分表（已包含 feedback_marks 列，合并 upgrade_assignment_feedback_marks 升级脚本）
DROP TABLE IF EXISTS `assignment_grade`;
CREATE TABLE `assignment_grade` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `submit_id`   BIGINT        NOT NULL COMMENT '提交ID',
  `score`       DECIMAL(5,2)  DEFAULT NULL COMMENT '评分',
  `comment`     TEXT          DEFAULT NULL COMMENT '评语',
  `feedback_marks` TEXT       DEFAULT NULL COMMENT '结构化批注JSON',
  `graded_by`   BIGINT        DEFAULT NULL COMMENT '评分教师ID',
  `graded_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '评分时间',
  `create_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT(1)    DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE INDEX `idx_submit_id` (`submit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业评分表';

-- ============================================================
-- 模块五：成绩（合并 upgrade_grade_nullable 升级脚本）
-- ============================================================

DROP TABLE IF EXISTS `grade_course`;
CREATE TABLE `grade_course` (
  `id`                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `course_id`           BIGINT        NOT NULL COMMENT '课程ID',
  `student_id`          BIGINT        NOT NULL COMMENT '学生用户ID',
  `exam_avg_score`      DECIMAL(5,2)  DEFAULT NULL COMMENT '考试平均分',
  `assignment_avg_score` DECIMAL(5,2)  DEFAULT NULL COMMENT '作业平均分',
  `total_score`         DECIMAL(5,2)  DEFAULT NULL COMMENT '总评成绩',
  `grade_level`         VARCHAR(20)   DEFAULT NULL COMMENT '等级：优秀 / 良好 / 中等 / 及格 / 不及格',
  `create_time`         DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`         DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`          TINYINT(1)    DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE INDEX `idx_course_student` (`course_id`, `student_id`),
  INDEX `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程总评成绩表';

-- ============================================================
-- 模块六：实训
-- ============================================================

DROP TABLE IF EXISTS `training_project`;
CREATE TABLE `training_project` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `project_name` VARCHAR(200) NOT NULL COMMENT '实训项目名称',
  `course_id`   BIGINT       DEFAULT NULL COMMENT '所属课程ID',
  `description` TEXT         DEFAULT NULL COMMENT '项目描述',
  `start_date`  DATE         DEFAULT NULL COMMENT '开始日期',
  `end_date`    DATE         DEFAULT NULL COMMENT '结束日期',
  `status`      VARCHAR(20)  DEFAULT '进行中' COMMENT '状态：进行中 / 已结束',
  `create_by`   BIGINT       DEFAULT NULL COMMENT '创建教师ID',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实训项目表';

DROP TABLE IF EXISTS `training_task`;
CREATE TABLE `training_task` (
  `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `project_id`     BIGINT        NOT NULL COMMENT '实训项目ID',
  `task_name`      VARCHAR(200)  NOT NULL COMMENT '任务名称',
  `description`    TEXT          DEFAULT NULL COMMENT '任务描述',
  `attachment_url` VARCHAR(500)  DEFAULT NULL COMMENT '附件URL',
  `deadline`       DATETIME      DEFAULT NULL COMMENT '截止时间',
  `sort_order`     INT           DEFAULT 0 COMMENT '排序号',
  `create_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`     TINYINT(1)    DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实训任务表';

DROP TABLE IF EXISTS `training_report`;
CREATE TABLE `training_report` (
  `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `task_id`        BIGINT       NOT NULL COMMENT '任务ID',
  `student_id`     BIGINT       NOT NULL COMMENT '学生用户ID',
  `report_content` TEXT         DEFAULT NULL COMMENT '报告内容',
  `attachment_url` VARCHAR(500) DEFAULT NULL COMMENT '附件URL',
  `submit_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `score`          DECIMAL(5,2) DEFAULT NULL COMMENT '评分',
  `comment`        TEXT         DEFAULT NULL COMMENT '评语',
  `status`         VARCHAR(20)  DEFAULT '待批改' COMMENT '状态：待批改 / 已批改',
  `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`     TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE INDEX `idx_task_student` (`task_id`, `student_id`),
  INDEX `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实训报告表';

-- ============================================================
-- 模块七：互动问答
-- ============================================================

DROP TABLE IF EXISTS `question_post`;
CREATE TABLE `question_post` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `course_id`   BIGINT       DEFAULT NULL COMMENT '所属课程ID',
  `title`       VARCHAR(200) NOT NULL COMMENT '帖子标题',
  `content`     TEXT         NOT NULL COMMENT '帖子内容',
  `post_type`   VARCHAR(20)  DEFAULT '提问' COMMENT '类型：提问 / 讨论',
  `create_by`   BIGINT       DEFAULT NULL COMMENT '发帖人用户ID',
  `view_count`  INT          DEFAULT 0 COMMENT '浏览次数',
  `reply_count` INT          DEFAULT 0 COMMENT '回复次数',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_course_id` (`course_id`),
  INDEX `idx_create_by` (`create_by`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='讨论帖/提问表';

DROP TABLE IF EXISTS `answer`;
CREATE TABLE `answer` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `post_id`     BIGINT       NOT NULL COMMENT '帖子ID',
  `content`     TEXT         NOT NULL COMMENT '回答内容',
  `create_by`   BIGINT       DEFAULT NULL COMMENT '回答人用户ID',
  `is_accepted` TINYINT(1)   DEFAULT 0 COMMENT '是否被采纳',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_post_id` (`post_id`),
  INDEX `idx_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='回答表';

DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `target_type` VARCHAR(20)  NOT NULL COMMENT '评论目标类型：POST / ANSWER',
  `target_id`   BIGINT       NOT NULL COMMENT '目标ID',
  `content`     TEXT         NOT NULL COMMENT '评论内容',
  `create_by`   BIGINT       DEFAULT NULL COMMENT '评论人用户ID',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_target` (`target_type`, `target_id`),
  INDEX `idx_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- ============================================================
-- 模块八：评价与问卷
-- ============================================================

DROP TABLE IF EXISTS `evaluation`;
CREATE TABLE `evaluation` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `course_id`       BIGINT       DEFAULT NULL COMMENT '课程ID',
  `evaluation_name` VARCHAR(200) NOT NULL COMMENT '评价名称',
  `evaluation_type` VARCHAR(20)  NOT NULL COMMENT '评价类型：教师评学 / 学生评教',
  `start_date`      DATE         DEFAULT NULL COMMENT '开始日期',
  `end_date`        DATE         DEFAULT NULL COMMENT '结束日期',
  `status`          VARCHAR(20)  DEFAULT '未开始' COMMENT '状态：未开始 / 进行中 / 已结束',
  `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价表';

DROP TABLE IF EXISTS `evaluation_item`;
CREATE TABLE `evaluation_item` (
  `id`            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `evaluation_id` BIGINT       NOT NULL COMMENT '评价表ID',
  `item_content`  VARCHAR(500) NOT NULL COMMENT '评价指标内容',
  `item_type`     VARCHAR(20)  DEFAULT 'RADIO' COMMENT '指标类型：RADIO（单选评分）/ TEXT（文本）',
  `sort_order`    INT          DEFAULT 0 COMMENT '排序号',
  `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_evaluation_id` (`evaluation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价指标项表';

DROP TABLE IF EXISTS `evaluation_result`;
CREATE TABLE `evaluation_result` (
  `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `evaluation_id`  BIGINT        NOT NULL COMMENT '评价表ID',
  `evaluator_id`   BIGINT        NOT NULL COMMENT '评价人用户ID',
  `target_user_id` BIGINT        NOT NULL COMMENT '被评价人用户ID',
  `item_id`        BIGINT        NOT NULL COMMENT '评价指标项ID',
  `score`          INT           DEFAULT NULL COMMENT '评分（1-5星）',
  `content`        TEXT          DEFAULT NULL COMMENT '文本评价内容',
  `create_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`     TINYINT(1)    DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_evaluation_id` (`evaluation_id`),
  INDEX `idx_evaluator_id` (`evaluator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价结果表';

DROP TABLE IF EXISTS `survey`;
CREATE TABLE `survey` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `title`       VARCHAR(200) NOT NULL COMMENT '问卷标题',
  `description` TEXT         DEFAULT NULL COMMENT '问卷说明',
  `course_id`   BIGINT       DEFAULT NULL COMMENT '所属课程ID',
  `start_date`  DATE         DEFAULT NULL COMMENT '开始日期',
  `end_date`    DATE         DEFAULT NULL COMMENT '结束日期',
  `status`      VARCHAR(20)  DEFAULT '未开始' COMMENT '状态：未开始 / 进行中 / 已结束',
  `create_by`   BIGINT       DEFAULT NULL COMMENT '创建教师ID',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问卷表';

DROP TABLE IF EXISTS `survey_question`;
CREATE TABLE `survey_question` (
  `id`               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `survey_id`        BIGINT       NOT NULL COMMENT '问卷ID',
  `question_content` VARCHAR(500) NOT NULL COMMENT '题目内容',
  `question_type`    VARCHAR(20)  NOT NULL COMMENT '题型：SINGLE / MULTI / TEXT',
  `options`          JSON         DEFAULT NULL COMMENT '选项（JSON格式）',
  `sort_order`       INT          DEFAULT 0 COMMENT '排序号',
  `is_required`      TINYINT(1)   DEFAULT 1 COMMENT '是否必填',
  `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`       TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_survey_id` (`survey_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问卷题目表';

DROP TABLE IF EXISTS `survey_response`;
CREATE TABLE `survey_response` (
  `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `survey_id`      BIGINT       NOT NULL COMMENT '问卷ID',
  `question_id`    BIGINT       NOT NULL COMMENT '题目ID',
  `respondent_id`  BIGINT       NOT NULL COMMENT '回答人用户ID',
  `answer_content` TEXT         DEFAULT NULL COMMENT '回答内容',
  `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`     TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_survey_id` (`survey_id`),
  INDEX `idx_respondent_id` (`respondent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问卷回答表';

-- ============================================================
-- 模块九：知识库与笔记
-- ============================================================

DROP TABLE IF EXISTS `knowledge_article`;
CREATE TABLE `knowledge_article` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `title`       VARCHAR(200) NOT NULL COMMENT '文章标题',
  `content`     LONGTEXT     DEFAULT NULL COMMENT '文章内容',
  `category`    VARCHAR(50)  DEFAULT NULL COMMENT '分类',
  `tags`        VARCHAR(500) DEFAULT NULL COMMENT '标签（逗号分隔）',
  `create_by`   BIGINT       DEFAULT NULL COMMENT '发布人用户ID',
  `view_count`  INT          DEFAULT 0 COMMENT '浏览次数',
  `status`      VARCHAR(20)  DEFAULT '发布' COMMENT '状态：草稿 / 发布',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_category` (`category`),
  INDEX `idx_create_by` (`create_by`),
  FULLTEXT INDEX `ft_title_content` (`title`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文章表';

DROP TABLE IF EXISTS `learning_note`;
CREATE TABLE `learning_note` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `student_id`      BIGINT       NOT NULL COMMENT '学生用户ID',
  `course_id`       BIGINT       DEFAULT NULL COMMENT '课程ID',
  `chapter_id`      BIGINT       DEFAULT NULL COMMENT '章节ID',
  `title`           VARCHAR(200) NOT NULL COMMENT '笔记标题',
  `content`         TEXT         DEFAULT NULL COMMENT '笔记内容',
  `video_timestamp` VARCHAR(20)  DEFAULT NULL COMMENT '关联视频时间点（如02:35）',
  `is_public`       TINYINT(1)   DEFAULT 0 COMMENT '是否公开分享',
  `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_student_id` (`student_id`),
  INDEX `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习笔记表';

-- ============================================================
-- 模块十：通知
-- ============================================================

DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
  `id`                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `title`             VARCHAR(200) NOT NULL COMMENT '通知标题',
  `content`           TEXT         DEFAULT NULL COMMENT '通知内容',
  `sender_id`         BIGINT       DEFAULT NULL COMMENT '发送者用户ID',
  `send_time`         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `notification_type` VARCHAR(20)  DEFAULT '系统' COMMENT '通知类型：系统 / 课程 / 考试 / 作业 / 公告 / 实训',
  `course_id`         BIGINT       DEFAULT NULL COMMENT '关联课程ID',
  `create_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`        TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_sender_id` (`sender_id`),
  INDEX `idx_send_time` (`send_time`),
  INDEX `idx_notification_type` (`notification_type`),
  INDEX `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

DROP TABLE IF EXISTS `notification_user`;
CREATE TABLE `notification_user` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `notification_id` BIGINT      NOT NULL COMMENT '通知ID',
  `user_id`         BIGINT      NOT NULL COMMENT '接收用户ID',
  `is_read`         TINYINT(1)  DEFAULT 0 COMMENT '是否已读',
  `read_time`       DATETIME    DEFAULT NULL COMMENT '阅读时间',
  `create_time`     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT(1)  DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_user_read` (`user_id`, `is_read`),
  INDEX `idx_notification_id` (`notification_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户通知状态表';

-- ============================================================
-- 模块十一：附件文件（合并 add_attachment_file_table 脚本）
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

-- ============================================================
-- 模块十二：随堂测验
-- ============================================================

DROP TABLE IF EXISTS `class_quiz`;
CREATE TABLE `class_quiz` (
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

DROP TABLE IF EXISTS `class_quiz_answer`;
CREATE TABLE `class_quiz_answer` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `quiz_id`     BIGINT      NOT NULL COMMENT '随堂测验ID',
  `question_id` BIGINT      DEFAULT NULL COMMENT '试题ID',
  `student_id`  BIGINT      NOT NULL COMMENT '学生用户ID',
  `answer`      TEXT        DEFAULT NULL COMMENT '学生答案',
  `is_correct`  TINYINT(1)  DEFAULT NULL COMMENT '是否正确：0错误 / 1正确',
  `submit_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT(1)  DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE INDEX `idx_quiz_student_question` (`quiz_id`, `student_id`, `question_id`),
  INDEX `idx_quiz_id` (`quiz_id`),
  INDEX `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='随堂测验作答表';

DROP TABLE IF EXISTS `class_quiz_question`;
CREATE TABLE `class_quiz_question` (
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

-- ============================================================
-- 模块十三：课堂互动积分
-- ============================================================

DROP TABLE IF EXISTS `participation_point`;
CREATE TABLE `participation_point` (
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

-- ============================================================
-- 模块十四：平台操作日志
-- ============================================================

DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
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

-- ============================================================
-- 初始数据
-- ============================================================

-- 角色初始数据
INSERT INTO `sys_role` (`role_code`, `role_name`) VALUES
('ROLE_ADMIN',    '管理员'),
('ROLE_TEACHER',  '教师'),
('ROLE_STUDENT',  '学生');

-- 管理员账号（密码: admin123，BCrypt 加密）
-- BCrypt hash for "admin123": $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh
INSERT INTO `sys_user` (`username`, `password`, `email`, `nickname`, `role_id`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', 'admin@kechang.com', '系统管理员',
 (SELECT id FROM `sys_role` WHERE `role_code` = 'ROLE_ADMIN'), 0);
