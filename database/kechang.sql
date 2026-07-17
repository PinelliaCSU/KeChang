/*
 Navicat Premium Dump SQL

 Source Server         : 本地
 Source Server Type    : MySQL
 Source Server Version : 90200 (9.2.0)
 Source Host           : localhost:3306
 Source Schema         : kechang

 Target Server Type    : MySQL
 Target Server Version : 90200 (9.2.0)
 File Encoding         : 65001

 Date: 15/07/2026 08:14:21
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for answer
-- ----------------------------
DROP TABLE IF EXISTS `answer`;
CREATE TABLE `answer`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '回答内容',
  `create_by` bigint NULL DEFAULT NULL COMMENT '回答人用户ID',
  `is_accepted` tinyint(1) NULL DEFAULT 0 COMMENT '是否被采纳',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_post_id`(`post_id` ASC) USING BTREE,
  INDEX `idx_create_by`(`create_by` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '回答表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of answer
-- ----------------------------
INSERT INTO `answer` VALUES (1, 1, 'rust', 3, 1, '2026-07-11 15:56:27', '2026-07-11 15:56:27', 0);
INSERT INTO `answer` VALUES (2, 1, 'c++\n', 2, 0, '2026-07-11 15:56:47', '2026-07-11 15:56:47', 0);
INSERT INTO `answer` VALUES (3, 1, '在选择 C++ 和 Rust 之间，关键在于项目需求和个人目标。C++ 是一种历史悠久、功能强大的语言，广泛用于系统开发、游戏引擎和高性能应用，拥有丰富的库和成熟的生态。但其复杂性较高，内存管理需要开发者自行处理，容易出现安全问题。\n\nRust 则以安全性为核心设计目标，通过独特的所有权机制，在编译时就能避免许多常见的错误，如空指针和数据竞争。它适合开发需要高可靠性和安全性的系统级程序，近年来在 WebAssembly 和嵌入式开发中表现突出。\n\n两者各有优势，C++ 更适合已有生态和性能要求极高的场景，而 Rust 更适合注重安全和现代编程范式的项目。建议根据具体应用场景和学习目标进行选择。如果需要更具体的对比或使用场景分析，欢迎补充更多细节。', 2, 0, '2026-07-15 08:01:27', '2026-07-15 08:01:27', 0);

-- ----------------------------
-- Table structure for assignment
-- ----------------------------
DROP TABLE IF EXISTS `assignment`;
CREATE TABLE `assignment`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `chapter_id` bigint NULL DEFAULT NULL COMMENT '所属章节ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '作业标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '作业内容/要求',
  `attachment_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '附件URL',
  `deadline` datetime NOT NULL COMMENT '截止时间',
  `total_score` decimal(5, 2) NULL DEFAULT 100.00 COMMENT '满分',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '进行中' COMMENT '状态：进行中 / 已截止',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建教师ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_course_id`(`course_id` ASC) USING BTREE,
  INDEX `idx_chapter_id`(`chapter_id` ASC) USING BTREE,
  INDEX `idx_deadline`(`deadline` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_course_status_deleted`(`course_id` ASC, `status` ASC, `is_deleted` ASC) USING BTREE,
  INDEX `idx_create_by_deleted`(`create_by` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '作业表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of assignment
-- ----------------------------
INSERT INTO `assignment` VALUES (1, 1, 1, '作业1', '', '', '2026-07-31 00:00:00', 100.00, '进行中', 2, NULL, NULL, 0);
INSERT INTO `assignment` VALUES (2, 1, 1, '作业2', '呃呃呃', '', '2026-07-30 00:00:00', 100.00, '进行中', 2, NULL, NULL, 0);
INSERT INTO `assignment` VALUES (3, 1, 1, '作业3', '烦烦烦', '', '2026-07-25 00:00:00', 100.00, '进行中', 2, NULL, NULL, 0);

-- ----------------------------
-- Table structure for assignment_grade
-- ----------------------------
DROP TABLE IF EXISTS `assignment_grade`;
CREATE TABLE `assignment_grade`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `submit_id` bigint NOT NULL COMMENT '提交ID',
  `score` decimal(5, 2) NULL DEFAULT NULL COMMENT '评分',
  `comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '评语',
  `feedback_marks` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '结构化批注JSON',
  `graded_by` bigint NULL DEFAULT NULL COMMENT '评分教师ID',
  `graded_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评分时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_submit_id`(`submit_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '作业评分表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of assignment_grade
-- ----------------------------
INSERT INTO `assignment_grade` VALUES (1, 1, 100.00, '好', '[]', 2, '2026-07-10 15:45:57', NULL, NULL, 0);
INSERT INTO `assignment_grade` VALUES (2, 2, 90.00, '', '[]', 2, '2026-07-10 16:17:07', NULL, NULL, 0);
INSERT INTO `assignment_grade` VALUES (3, 3, 60.00, '', '[]', 2, '2026-07-13 13:56:30', '2026-07-13 13:56:30', '2026-07-13 13:56:30', 0);

-- ----------------------------
-- Table structure for assignment_submit
-- ----------------------------
DROP TABLE IF EXISTS `assignment_submit`;
CREATE TABLE `assignment_submit`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `assignment_id` bigint NOT NULL COMMENT '作业ID',
  `student_id` bigint NOT NULL COMMENT '学生用户ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '提交内容',
  `attachment_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '附件URL',
  `submit_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `score` decimal(5, 2) NULL DEFAULT NULL COMMENT '得分',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '待批改' COMMENT '状态：待批改 / 已批改',
  `is_excellent` tinyint(1) NULL DEFAULT 0 COMMENT '是否优秀作业',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_assignment_student`(`assignment_id` ASC, `student_id` ASC) USING BTREE,
  INDEX `idx_student_id`(`student_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_assignment_status_deleted`(`assignment_id` ASC, `status` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '作业提交表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of assignment_submit
-- ----------------------------
INSERT INTO `assignment_submit` VALUES (1, 1, 3, '作业一答案。', '', '2026-07-10 15:45:32', 100.00, '已批改', 0, NULL, NULL, 0);
INSERT INTO `assignment_submit` VALUES (2, 2, 3, '哈哈哈哈', '', '2026-07-10 16:16:32', 90.00, '已批改', 0, NULL, NULL, 0);
INSERT INTO `assignment_submit` VALUES (3, 3, 3, '', '/uploads/common/4ce0a305-807f-41a8-950d-48b8daf5bc55_8209230308-刘麒源-学生实训日志.docx', '2026-07-13 11:39:48', 60.00, '已批改', 0, '2026-07-13 11:39:48', '2026-07-13 11:39:48', 0);

-- ----------------------------
-- Table structure for attachment_file
-- ----------------------------
DROP TABLE IF EXISTS `attachment_file`;
CREATE TABLE `attachment_file`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `target_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标类型：ASSIGNMENT(作业附件) / SUBMIT(提交附件)',
  `target_id` bigint NOT NULL COMMENT '目标ID（作业ID或提交ID）',
  `file_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始文件名',
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件访问URL',
  `file_size` bigint NULL DEFAULT 0 COMMENT '文件大小（字节）',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序号',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_target`(`target_type` ASC, `target_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '附件文件表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of attachment_file
-- ----------------------------
INSERT INTO `attachment_file` VALUES (1, 'SUBMIT', 3, '8209230308-刘麒源-学生实训日志.docx', '/uploads/common/4ce0a305-807f-41a8-950d-48b8daf5bc55_8209230308-刘麒源-学生实训日志.docx', 84838, 0, '2026-07-13 11:39:48', '2026-07-13 11:39:48', 0);

-- ----------------------------
-- Table structure for checkin_record
-- ----------------------------
DROP TABLE IF EXISTS `checkin_record`;
CREATE TABLE `checkin_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` bigint NOT NULL COMMENT '关联签到场次ID',
  `student_id` bigint NOT NULL COMMENT '学生用户ID',
  `checkin_time` datetime NOT NULL COMMENT '签到时间',
  `checkin_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '实际使用方式（同session的checkin_type）',
  `latitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '学生签到时的纬度',
  `longitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '学生签到时的经度',
  `status` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SUCCESS' COMMENT '签到状态：SUCCESS / LATE / ABSENT',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除：0正常 / 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_session_student`(`session_id` ASC, `student_id` ASC) USING BTREE,
  INDEX `idx_student_id`(`student_id` ASC) USING BTREE,
  INDEX `idx_session_id`(`session_id` ASC) USING BTREE,
  INDEX `idx_checkin_time`(`checkin_time` ASC) USING BTREE,
  INDEX `idx_session_deleted`(`session_id` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '签到记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of checkin_record
-- ----------------------------
INSERT INTO `checkin_record` VALUES (1, 1, 3, '2026-07-10 17:40:22', 'NUMERIC_CODE', NULL, NULL, 'SUCCESS', NULL, 0);
INSERT INTO `checkin_record` VALUES (2, 2, 3, '2026-07-10 17:41:50', 'NUMERIC_CODE', NULL, NULL, 'SUCCESS', NULL, 0);
INSERT INTO `checkin_record` VALUES (3, 3, 3, '2026-07-10 17:42:34', 'LOCATION', 30.7921714, 103.8364115, 'SUCCESS', NULL, 0);
INSERT INTO `checkin_record` VALUES (4, 4, 3, '2026-07-10 18:29:39', NULL, NULL, NULL, 'ABSENT', NULL, 0);
INSERT INTO `checkin_record` VALUES (5, 5, 3, '2026-07-13 13:23:47', 'NUMERIC_CODE', NULL, NULL, 'SUCCESS', '2026-07-13 13:23:47', 0);
INSERT INTO `checkin_record` VALUES (6, 6, 3, '2026-07-13 13:31:12', 'NUMERIC_CODE', NULL, NULL, 'SUCCESS', '2026-07-13 13:31:12', 0);
INSERT INTO `checkin_record` VALUES (7, 7, 3, '2026-07-13 13:38:12', 'NUMERIC_CODE', NULL, NULL, 'SUCCESS', '2026-07-13 13:38:12', 0);

-- ----------------------------
-- Table structure for checkin_session
-- ----------------------------
DROP TABLE IF EXISTS `checkin_session`;
CREATE TABLE `checkin_session`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '课堂签到' COMMENT '签到标题',
  `checkin_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NUMERIC_CODE' COMMENT '签到方式：NUMERIC_CODE / LOCATION',
  `code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '6位数字签到码',
  `latitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '地理位置签到纬度（LOCATION方式）',
  `longitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '地理位置签到经度（LOCATION方式）',
  `location_radius` int NULL DEFAULT 100 COMMENT '有效半径（米），默认100',
  `start_time` datetime NOT NULL COMMENT '签到开始时间',
  `end_time` datetime NOT NULL COMMENT '签到截止时间',
  `status` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE / ENDED',
  `create_by` bigint NOT NULL COMMENT '发起教师ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除：0正常 / 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_course_id`(`course_id` ASC) USING BTREE,
  INDEX `idx_create_by`(`create_by` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_code`(`code` ASC) USING BTREE,
  INDEX `idx_start_time`(`start_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '签到场次表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of checkin_session
-- ----------------------------
INSERT INTO `checkin_session` VALUES (1, 1, '测试签到', 'NUMERIC_CODE', '950736', NULL, NULL, 100, '2026-07-10 17:40:01', '2026-07-10 17:41:01', 'ENDED', 2, NULL, NULL, 0);
INSERT INTO `checkin_session` VALUES (2, 1, '测试数字码签到', 'NUMERIC_CODE', '656257', NULL, NULL, 100, '2026-07-10 17:41:23', '2026-07-10 17:44:23', 'ENDED', 2, NULL, NULL, 0);
INSERT INTO `checkin_session` VALUES (3, 1, '测试位置签到', 'LOCATION', '486708', 30.7921714, 103.8364115, 100, '2026-07-10 17:42:24', '2026-07-10 17:47:24', 'ENDED', 2, NULL, NULL, 0);
INSERT INTO `checkin_session` VALUES (4, 1, '数字码测试', 'NUMERIC_CODE', '409861', NULL, NULL, 100, '2026-07-10 18:24:39', '2026-07-10 18:29:39', 'ENDED', 2, NULL, NULL, 0);
INSERT INTO `checkin_session` VALUES (5, 2, '数据库签到', 'NUMERIC_CODE', '575528', NULL, NULL, 100, '2026-07-13 13:23:31', '2026-07-13 13:26:31', 'ENDED', 2, '2026-07-13 13:23:31', '2026-07-13 13:23:31', 0);
INSERT INTO `checkin_session` VALUES (6, 2, '时间签到', 'NUMERIC_CODE', '496340', NULL, NULL, 100, '2026-07-13 13:31:00', '2026-07-13 13:34:00', 'ENDED', 2, '2026-07-13 13:31:00', '2026-07-13 13:31:00', 0);
INSERT INTO `checkin_session` VALUES (7, 2, '测试Ui', 'NUMERIC_CODE', '954379', NULL, NULL, 100, '2026-07-13 13:38:03', '2026-07-13 13:41:03', 'ENDED', 2, '2026-07-13 13:38:03', '2026-07-13 13:38:03', 0);

-- ----------------------------
-- Table structure for class_quiz
-- ----------------------------
DROP TABLE IF EXISTS `class_quiz`;
CREATE TABLE `class_quiz`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `question_id` bigint NOT NULL COMMENT '题库试题ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '测验标题',
  `duration_seconds` int NULL DEFAULT 120 COMMENT '限时秒数',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE / ENDED',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建教师ID',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_course_id`(`course_id` ASC) USING BTREE,
  INDEX `idx_question_id`(`question_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_by`(`create_by` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '随堂测验会话表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of class_quiz
-- ----------------------------
INSERT INTO `class_quiz` VALUES (1, 1, 1, '测试', 120, 'ENDED', 2, '2026-07-10 15:13:31', '2026-07-10 15:13:57', NULL, NULL, 0);
INSERT INTO `class_quiz` VALUES (2, 1, 1, '帆帆帆帆', 120, 'ENDED', 2, '2026-07-11 16:10:26', '2026-07-11 16:10:30', '2026-07-11 16:10:26', '2026-07-11 16:10:26', 0);

-- ----------------------------
-- Table structure for class_quiz_answer
-- ----------------------------
DROP TABLE IF EXISTS `class_quiz_answer`;
CREATE TABLE `class_quiz_answer`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `quiz_id` bigint NOT NULL COMMENT '随堂测验ID',
  `question_id` bigint NULL DEFAULT NULL COMMENT '试题ID',
  `student_id` bigint NOT NULL COMMENT '学生用户ID',
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '学生答案',
  `is_correct` tinyint(1) NULL DEFAULT NULL COMMENT '是否正确：0错误 / 1正确',
  `submit_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_quiz_student_question`(`quiz_id` ASC, `student_id` ASC, `question_id` ASC) USING BTREE,
  INDEX `idx_quiz_id`(`quiz_id` ASC) USING BTREE,
  INDEX `idx_student_id`(`student_id` ASC) USING BTREE,
  INDEX `idx_quiz_student_deleted`(`quiz_id` ASC, `student_id` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '随堂测验作答表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of class_quiz_answer
-- ----------------------------
INSERT INTO `class_quiz_answer` VALUES (1, 1, 1, 3, 'B', 0, '2026-07-10 15:13:40', NULL, NULL, 0);

-- ----------------------------
-- Table structure for class_quiz_question
-- ----------------------------
DROP TABLE IF EXISTS `class_quiz_question`;
CREATE TABLE `class_quiz_question`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `quiz_id` bigint NOT NULL COMMENT '随堂测验ID',
  `question_id` bigint NOT NULL COMMENT '试题ID',
  `sort_order` int NULL DEFAULT 0 COMMENT '题目顺序',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_quiz_question`(`quiz_id` ASC, `question_id` ASC) USING BTREE,
  INDEX `idx_quiz_id`(`quiz_id` ASC) USING BTREE,
  INDEX `idx_question_id`(`question_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '随堂测验题目关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of class_quiz_question
-- ----------------------------
INSERT INTO `class_quiz_question` VALUES (1, 1, 1, 1, NULL, NULL, 0);
INSERT INTO `class_quiz_question` VALUES (2, 2, 1, 1, '2026-07-11 16:10:26', '2026-07-11 16:10:26', 0);

-- ----------------------------
-- Table structure for code_submission
-- ----------------------------
DROP TABLE IF EXISTS `code_submission`;
CREATE TABLE `code_submission`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `exam_id` bigint NULL DEFAULT NULL COMMENT '考试ID（考试内提交时有值）',
  `record_id` bigint NULL DEFAULT NULL COMMENT '考试记录ID',
  `language` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '编程语言：c/cpp/python',
  `source_code` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提交的源代码',
  `result_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '判题结果：AC/WA/TLE/MLE/RE/CE',
  `passed_cases` int NULL DEFAULT 0 COMMENT '通过用例数',
  `total_cases` int NULL DEFAULT 0 COMMENT '总用例数',
  `compile_error` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '编译错误信息',
  `execution_time` int NULL DEFAULT NULL COMMENT '最大执行时间(ms)',
  `execution_memory` int NULL DEFAULT NULL COMMENT '最大内存使用(KB)',
  `case_details` json NULL COMMENT '每个用例的详细结果',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_student`(`student_id` ASC) USING BTREE,
  INDEX `idx_question`(`question_id` ASC) USING BTREE,
  INDEX `idx_record`(`record_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '代码提交记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of code_submission
-- ----------------------------

-- ----------------------------
-- Table structure for comment
-- ----------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `target_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论目标类型：POST / ANSWER',
  `target_id` bigint NOT NULL COMMENT '目标ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论内容',
  `create_by` bigint NULL DEFAULT NULL COMMENT '评论人用户ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_target`(`target_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `idx_create_by`(`create_by` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评论表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of comment
-- ----------------------------

-- ----------------------------
-- Table structure for course
-- ----------------------------
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '课程名称',
  `cover_img` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '封面图片URL',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '课程简介',
  `teacher_id` bigint NOT NULL COMMENT '授课教师ID（关联sys_user）',
  `start_date` date NULL DEFAULT NULL COMMENT '开课日期',
  `end_date` date NULL DEFAULT NULL COMMENT '结课日期',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '进行中' COMMENT '课程状态：进行中 / 已结束',
  `student_count` int NULL DEFAULT 0 COMMENT '选课人数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_teacher_id`(`teacher_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_course_name`(`course_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '课程主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of course
-- ----------------------------
INSERT INTO `course` VALUES (1, 'c++', '/uploads/common/9433ce7c-0cf4-479f-93c6-53e8bb2f903d_logo.jpg', '', 2, '2026-07-09', '2026-07-18', '进行中', 1, NULL, NULL, 0);
INSERT INTO `course` VALUES (2, '数据库系统', '', '', 2, '2026-07-11', '2026-07-31', '进行中', 1, '2026-07-11 20:16:30', '2026-07-11 20:16:30', 0);

-- ----------------------------
-- Table structure for course_chapter
-- ----------------------------
DROP TABLE IF EXISTS `course_chapter`;
CREATE TABLE `course_chapter`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `chapter_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '章节名称',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序号',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '章节描述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_course_id`(`course_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '课程章节表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of course_chapter
-- ----------------------------
INSERT INTO `course_chapter` VALUES (1, 1, '章节一', 1, '', NULL, NULL, 0);

-- ----------------------------
-- Table structure for course_knowledge_chunk
-- ----------------------------
DROP TABLE IF EXISTS `course_knowledge_chunk`;
CREATE TABLE `course_knowledge_chunk`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `document_id` bigint NOT NULL COMMENT '文档ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `chunk_index` int NOT NULL COMMENT '文档内分块序号',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分块文本',
  `source_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '来源标题',
  `source_type` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '来源类型',
  `source_id` bigint NULL DEFAULT NULL COMMENT '来源业务ID',
  `source_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '来源文件或页面地址',
  `page_no` int NULL DEFAULT NULL COMMENT '页码：PDF/PPT 等分页资料',
  `start_time` int NULL DEFAULT NULL COMMENT '视频/音频分段开始秒数',
  `end_time` int NULL DEFAULT NULL COMMENT '视频/音频分段结束秒数',
  `section_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '章节/标题/题目等语义分段标题',
  `token_count` int NULL DEFAULT 0 COMMENT '粗略 token/字符数量',
  `metadata_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '来源扩展元数据 JSON',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_document`(`document_id` ASC) USING BTREE,
  INDEX `idx_course`(`course_id` ASC) USING BTREE,
  INDEX `idx_source`(`source_type` ASC, `source_id` ASC) USING BTREE,
  INDEX `idx_course_knowledge_chunk_page`(`page_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 39 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '课程知识库分块' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of course_knowledge_chunk
-- ----------------------------
INSERT INTO `course_knowledge_chunk` VALUES (1, 1, 2, 0, '题目：已有 student 表结构如下：\n  CREATE TABLE student (id INT, name TEXT, score INT);\n已插入以下数据：\n  INSERT INTO student VALUES (1, \'张三\', 85), (2, \'李四\', 92), (3, \'王五\', 78), (4, \'赵六\', 88), (5, \'孙七\', 95);\n\n请编写 SQL 查询：查询成绩大于等于 85 分的学生姓名和成绩，按成绩降序排列。\n\n输出格式：\n每行一个学生，格式为 \"姓名|成绩\"。\n选项：\n答案：\n解析：使用 WHERE 子句过滤 score >= 85，使用 ORDER BY 降序排列。注意输出格式需要拼接字段。\n知识点：SQL-查询语句', '题库解析 #41', 'QUESTION', 41, NULL, NULL, NULL, NULL, '题目 #41', 141, '{\"questionType\":\"CODE\",\"questionId\":41,\"knowledgePoint\":\"SQL-查询语句\",\"difficulty\":2,\"sourceType\":\"QUESTION\",\"sourceId\":41}', '2026-07-14 12:24:17', '2026-07-14 12:24:17', 0);
INSERT INTO `course_knowledge_chunk` VALUES (2, 2, 1, 0, '[ASR 失败] 资源「屏幕录制 2026-07-11 144838.mp4」音频转写失败：Cannot run program \"D:/ffmpeg/ffmpeg-8.1.2-essentials_build/bin/ffmpeg.exe\": CreateProcess error=2, 系统找不到指定的文件。', '屏幕录制 2026-07-11 144838.mp4', 'RESOURCE', 1, '/uploads/courses/1/6adc1c09-cd23-48d5-997e-f315250bf710_屏幕录制 2026-07-11 144838.mp4', NULL, 0, 120, NULL, 47, '{\"extension\":\"mp4\",\"resourceType\":\"VIDEO\",\"chapterId\":1,\"timeSource\":\"estimated\",\"estimatedSecondsPerChunk\":120,\"sourceType\":\"RESOURCE\",\"sourceId\":1}', '2026-07-14 12:24:30', '2026-07-14 12:24:30', 0);
INSERT INTO `course_knowledge_chunk` VALUES (3, 3, 1, 0, '学生实习日志\n1、本日志供学生实习记录使用，不作实习总结报告用途。\n2、实习结束，学生须将实习日志统一交实习队指导教师。\n本科生院制\n中南大学本科实习教学管理办法\n（2018年6月28日第十四次校务会议讨论通过）\n(中大教字[2018]46号)\n第一章  实习组织管理\n第一条  实习教学是学校教学工作和人才培养的重要环节，为进一步加强和规范实习教学管理，提高实习教学效果和质量，结合我校实际，特制定本办法。\n第二条  实习教学工作在主管校长领导下，实行学校、二级单位、系(所、中心)三级管理。\n第三条  本科生院归口负责全校实习教学管理工作。负责制定实习教学规章制度与工作规划、实习经费使用监督，部署实习教学工作，建立实习安全保障机制，组织实习教学检查、实习教师申报教学质量优秀奖评审、实习总结交流与实习教学质量评价，协调处理实习教学中的相关问题。\n第四条  二级单位负责组织实施本单位实习教学工作，制定本单位学生实习教学实施细则、实习经费使用办法和实习教学工作安排，落实实习教学任务，做好师生实习动员与安全纪律教育，组织实习教学质量考核(含申优考核)、本单位实习教学自查自评和工作总结。\n第五条  系(所、中心)负责制定实习教学大纲和实习指导书，选派实习指导教师，监督实习教学的实施，推荐实习教师申报教学质量优秀奖。\n第六条  实习指导教师和实习队委会全面负责学生的现场实习教学、生活保障、实习安全、实习学生的思想政治教育与管理工作，做好实习教学实施方案、实习经费使用管理及与校外基地(实习单位)的沟通协调工作。\n第二章  实习教学类别与形式\n第七条  实习教学类别分为：认识实习、课程教学实习(调研实践)、工程实习训练、生产(专业)实习、毕业实习(含临床实习)等；\n第八条  实习教学的形式分为集中实习与自主实习；\n第九条  实习教学按实习地点分为校内实习与校外实习。\n第三章  实习教学资料\n第十条  实习教学文件资料包括：实习教学大纲、实习指导书、实习教材视频讲义、实习教学实施方案、学生实习日志与实习(调研)报告、二级单位实习工作安排、实习计划与实习工作总结、实习队实习记录与总结材料等。', '8209230308-刘麒源-学生实训日志.docx', 'RESOURCE', 2, '/uploads/courses/1/4fedba5a-86fe-45f2-ae54-8887883df964_8209230308-刘麒源-学生实训日志.docx', NULL, NULL, NULL, '学生实习日志', 765, '{\"extension\":\"docx\",\"resourceType\":\"LINK\",\"chapterId\":1,\"splitStart\":0,\"splitEnd\":900,\"sourceType\":\"RESOURCE\",\"sourceId\":2}', '2026-07-14 12:24:30', '2026-07-14 12:24:30', 0);
INSERT INTO `course_knowledge_chunk` VALUES (4, 3, 1, 1, '习与校外实习。\n第三章  实习教学资料\n第十条  实习教学文件资料包括：实习教学大纲、实习指导书、实习教材视频讲义、实习教学实施方案、学生实习日志与实习(调研)报告、二级单位实习工作安排、实习计划与实习工作总结、实习队实习记录与总结材料等。\n第十一条  实习教学大纲由二级单位、系（所、中心）根据人才培养方案和学科专业特点制定。实习教学大纲的内容主要包括：1.实习性质、目的和任务；2.基本要求；3.实习内容与实习形式；4.实习时间(周数)安排；5.实习考核方式与评分办法等。\n第十二条  实习指导书、实习教材视频讲义等由系（所、中心）根据专业培养目标、实习教学大纲等要求联合校外基地(实习单位)共同编制，是学生实习详细的指导性教学资料。\n第十三条  实习教学实施方案由各实习队根据实习教学大纲和校外基地(实习单位)的实际情况制定，包括实习队具体的工作安排、教学安排与经费使用方案等。\n第四章  实习教学实施\n第十四条  二级单位每年根据本科人才培养方案和学校的相关工作要求，制定本单位实习教学工作安排和年度实习教学计划安排表，经二级单位主管领导审核签字后于每年3月底报本科生院。实习开始两周前将实习教学实施方案报本科生院。\n第十五条  二级单位应按照人才培养方案和校历严格保证学生的实习时间，不得以各种理由缩减或变相减少学生的现场实习时间。\n第十六条  每学生班应安排不少于两名校内指导教师，二级单位应选派实习教学经验丰富、熟悉实习单位情况的教师担任实习指导工作，优先安排教授、副教授参与实习教学，中级与初级职称教师不得独立承担实习队指导任务。\n第十七条  同一时间相同专业在同一校外基地(实习单位)的学生组成实习队，由二级单位指定其中一位指导教师担任实习队负责人。各实习队成立包括队长、安全委员、生活委员在内的队委会，协助实习队负责人和指导教师管理实习期间的相关事务。\n第十八条  二级单位实习前应组织实习动员，宣讲实习目的与要求，介绍校外基地(实习单位)情况，进行安全保密与纪律教育，统一为实习师生购买实习保险。实习指导教师实习前应领取《实习队实习情况记录册》，学生以班为单位到学校教材供应部门', '8209230308-刘麒源-学生实训日志.docx', 'RESOURCE', 2, '/uploads/courses/1/4fedba5a-86fe-45f2-ae54-8887883df964_8209230308-刘麒源-学生实训日志.docx', NULL, NULL, NULL, '学生实习日志', 792, '{\"extension\":\"docx\",\"resourceType\":\"LINK\",\"chapterId\":1,\"splitStart\":780,\"splitEnd\":1680,\"sourceType\":\"RESOURCE\",\"sourceId\":2}', '2026-07-14 12:24:30', '2026-07-14 12:24:30', 0);
INSERT INTO `course_knowledge_chunk` VALUES (5, 3, 1, 2, '的相关事务。\n第十八条  二级单位实习前应组织实习动员，宣讲实习目的与要求，介绍校外基地(实习单位)情况，进行安全保密与纪律教育，统一为实习师生购买实习保险。实习指导教师实习前应领取《实习队实习情况记录册》，学生以班为单位到学校教材供应部门统一购买《学生实习日志》。\n第十九条  实习期间实习队负责人与实习队委会全权处理实习队的各项事务和有关问题，认真填写《实习队实习情况记录册》，及时向学校和二级单位反馈实习情况、通报有关信息。实习指导教师负责检查评阅学生实习日志、实习报告并评定实习成绩。实习结束实习队应及时做好实习工作总结并请校外基地(实习单位)签署意见，返校后及时交所在二级单位。\n第二十条  实习期间，在完成实习教学大纲规定的各项内容、任务的同时，学校鼓励实习队组织学生积极参加当地的社会公益活动和科技文化交流，如社区服务、科技文化互动、公益劳动和文体活动等。\n第五章  学生自主实习\n第二十一条  学生自主实习是一种个性化的实习教学形式。自主实习应与集中实习的内容、要求保持一致。\n第二十二条  二级单位负责学生自主实习的审批、校内指导教师安排、自主实习组织实施和质量跟踪监控。必须加强对自主实习教学的组织领导，重视学生安全、纪律和保密教育，严格实习要求和质量监控，制定远程跟踪指导和定期报告制度、不得放任自流。\n第二十三条  二级单位应安排实习指导经验丰富、工作认真负责的教师作为自主实习学生的校内指导教师，同时对学生进行自主实习前的约谈和教育(包括实习纪律教育、安全环保教育、保密教育、劳动保护法教育等)。\n第二十四条  申请自主实习的学生必须事先取得家长同意并在二级单位规定的时间内提交下列申请材料：《中南大学学生自主实习申请审批表》(附件4)、实习单位书面接收函、国内外实习任职交流资格材料与推荐材料、校外指导教师材料、自主实习岗位与实习内容安排等。经二级单位批准后签订《中南大学学生自主实习安全责任协议》（附件5），并报本科生院备案。\n第二十五条  自主实习学生的校内指导教师要根据实习教学大纲、实习指导书、学生和实习单位的具体情况制定科学、合理的自主实习教学实施方案，明确学生', '8209230308-刘麒源-学生实训日志.docx', 'RESOURCE', 2, '/uploads/courses/1/4fedba5a-86fe-45f2-ae54-8887883df964_8209230308-刘麒源-学生实训日志.docx', NULL, NULL, NULL, '学生实习日志', 800, '{\"extension\":\"docx\",\"resourceType\":\"LINK\",\"chapterId\":1,\"splitStart\":1560,\"splitEnd\":2460,\"sourceType\":\"RESOURCE\",\"sourceId\":2}', '2026-07-14 12:24:31', '2026-07-14 12:24:31', 0);
INSERT INTO `course_knowledge_chunk` VALUES (6, 3, 1, 3, '容安排等。经二级单位批准后签订《中南大学学生自主实习安全责任协议》（附件5），并报本科生院备案。\n第二十五条  自主实习学生的校内指导教师要根据实习教学大纲、实习指导书、学生和实习单位的具体情况制定科学、合理的自主实习教学实施方案，明确学生的实习任务和教学要求，确定远程指导、过程监督方式及与校外指导教师沟通联系的途径。\n第二十六条  学生自主实习期间应服从实习单位的管理和指导，认真填写实习日志，定期向校内指导教师和二级单位学工部门汇报实习和个人情况，认真完成实习教学大纲、实习指导书和校内、外指导教师规定的实习教学内容和工作任务。\n第二十七条  自主实习结束后，学生应将实习日志、实习报告、实习单位鉴定材料、个人总结等材料提交给二级单位和校内指导教师。二级单位和校内指导教师根据学生提交的材料和实习过程定期汇报情况等内容进行综合评定，或由二级单位组织自主实习学生集中进行实习答辩等方式评定。\n第六章  实习纪律与成绩考核\n第二十八条  实习学生应自觉遵守国家法令和学校、校外基地(实习单位)的各项规章制度。对违反纪律的学生与行为，实习队负责人应及时给予批评教育，经教育无效或情节严重者，暂停其实习，并及时报告二级单位和学校有关部门按校规校纪严肃处理。\n第二十九条  严格实行考勤与请假制度。实习期间的考勤按学校和校外基地(实习单位)的相关规定执行，原则上不得请假。实习期间学生因病、事确需请假的，应持相关证明向实习队提出，两天以内由实习队负责人批准，超过两天的实习队同意后须请示二级单位教学与学工负责人共同批准方可准假。未经准假不参加实习者按旷课处理。\n第三十条  实习考核成绩按五级计分制(优秀、良好、中等、及格、不及格)记分，成绩评定应服从正态分布，其中优秀率不得大于25%、优良率不得大于70%。考核内容包括：学生实习日志、实习(调研)报告、实习态度与出勤表现、现场操作情况、校外基地(实习单位)评价等。\n第三十一条  凡请假或缺实习达到实习总时间三分之一及以上、实习(调研)报告存在严重错误或弄虚作假行为、实习期间发生较大责任事故、校外基地(实习单位)评价极差、实习未能达到实习教学大纲规', '8209230308-刘麒源-学生实训日志.docx', 'RESOURCE', 2, '/uploads/courses/1/4fedba5a-86fe-45f2-ae54-8887883df964_8209230308-刘麒源-学生实训日志.docx', NULL, NULL, NULL, '学生实习日志', 797, '{\"extension\":\"docx\",\"resourceType\":\"LINK\",\"chapterId\":1,\"splitStart\":2340,\"splitEnd\":3240,\"sourceType\":\"RESOURCE\",\"sourceId\":2}', '2026-07-14 12:24:31', '2026-07-14 12:24:31', 0);
INSERT INTO `course_knowledge_chunk` VALUES (7, 3, 1, 4, '与出勤表现、现场操作情况、校外基地(实习单位)评价等。\n第三十一条  凡请假或缺实习达到实习总时间三分之一及以上、实习(调研)报告存在严重错误或弄虚作假行为、实习期间发生较大责任事故、校外基地(实习单位)评价极差、实习未能达到实习教学大纲规定的基本要求者，其实习成绩以不及格记。\n第三十二条  对单独设课的实习教学环节，其成绩直接作为该课程成绩。非单独设课的实习，其成绩按一定的比例(具体比例由二级单位研究确定并列入课程教学大纲或实习教学大纲)计入相应课程的学期总成绩，如实习考核成绩不及格，不能参加该门课程的期末考核。\n第三十三条  如学生对自主实习成绩评定有疑义，应当在知道或者应当知道评定结果后15日内向所在二级单位提出书面复查申请，经二级单位主管领导签字同意，报本科生院审批后，由二级单位组织成绩复查并将复查结果及时通知学生本人。\n第七章  实习教学经费管理\n第三十四条  实习教学经费按学校有关规定从二级单位年度教学经费中划拨，单列帐户专款专用。学校鼓励二级单位从其他渠道筹措补充实习教学经费。\n第三十五条  实习教学经费主要用于师生实习差旅和生活补贴、实习检查与联络差旅费、现场指导费和讲课费、实习管理费、实习防护用品费、实习保险等。有关开支按照学校财务统一规定执行。各二级单位可制定相关费用开支、补助标准报学校计划财务处及本科生院备案施行。\n第三十六条  二级单位应采取措施提高学生的实习补助标准，实习队应尽可能争取校外基地(实习单位)的支持和优惠，确保经费使用公开、透明，减轻学生与家长的经济负担，确保良好的实习教学效果。\n第八章  实习教学改革\n第三十七条  学校支持二级单位、系(所、中心)、教师积极开展实习综合改革研究并将其纳入学校各类教改课题的立项申报。\n第三十八条  鼓励实习指导教师进行实习教学内容、教学方法的改革和研究，支持实习指导教师进行不同实习教学模式的探索与实践。\n第九章  实习教学检查评价与总结\n第三十九条  学校定期组织实习教学检查评价，内容包括：1.实习组织管理；2.实习教学大纲、实习教学实施方案的执行情况；3.实习教学质量与效果；4.实习纪律与安全、保', '8209230308-刘麒源-学生实训日志.docx', 'RESOURCE', 2, '/uploads/courses/1/4fedba5a-86fe-45f2-ae54-8887883df964_8209230308-刘麒源-学生实训日志.docx', NULL, NULL, NULL, '学生实习日志', 795, '{\"extension\":\"docx\",\"resourceType\":\"LINK\",\"chapterId\":1,\"splitStart\":3120,\"splitEnd\":4020,\"sourceType\":\"RESOURCE\",\"sourceId\":2}', '2026-07-14 12:24:31', '2026-07-14 12:24:31', 0);
INSERT INTO `course_knowledge_chunk` VALUES (8, 3, 1, 5, '导教师进行不同实习教学模式的探索与实践。\n第九章  实习教学检查评价与总结\n第三十九条  学校定期组织实习教学检查评价，内容包括：1.实习组织管理；2.实习教学大纲、实习教学实施方案的执行情况；3.实习教学质量与效果；4.实习纪律与安全、保密工作；5.其它。\n第四十条  实习教学检查评价采取学校专家组检查评价与二级单位自查评价相结合的方式进行，对立项建设的校外基地实习教学工作进行重点检查和考评。学校检查评价由本科生院组织专家与教学管理人员深入校外基地(实习单位)，对学生实习情况进行现场考察检查，与校外基地(实习单位)和师生进行沟通、座谈，并开展问卷调查。\n第四十一条  实习结束后，各实习队要及时、认真做好实习总结，总结材料应包括：实习教学大纲和实习教学实施方案的执行情况、质量分析、经验体会、存在问题、解决措施、意见建议等。\n第四十二条  二级单位每年度应对全年的实习教学工作进行认真总结和分析，组织实习队进行经验交流并考评，每年12月下旬应向本科生院送交本单位年度实习工作总结。\n第四十三条  为鼓励广大教师积极承担实习教学工作，实习指导教师可自愿申报学校教学质量优秀奖，采取个人申报、系(所、中心)和二级单位审查推荐、学校专家组评审的方式进行。\n第十章  附则\n第四十四条  本办法自2018年7月1日起施行，由本科生院负责日常解释工作。《中南大学本科实习教学管理办法》(中大教字[2011]22号)、《中南大学学生自主实习管理办法》(中大教字[2011]21号)同时废止。\n附件1：\n中南大学学生实习纪律\n一、严格遵守国家法律法规，遵守学校和校外基地(实习单位)的各项有关规章制度。\n二、服从实习队(负责人)和校外基地(实习单位)的管理，遵守工作与劳动纪律，不迟到、不早退、不无故缺席。\n三、实习期间原则上不得请假。学生因病、事确需请假的，应持相关证明向实习队提出，两天以内由实习队负责人批准，超过两天的实习队同意后须请示二级单位教学与学工负责人共同批准方可准假。未经准假不参加实习者按旷课处理（医学生按医学实习有关规定执行）。\n四、实习期间，学生不允许离队外宿。\n五、自觉践行社会主义', '8209230308-刘麒源-学生实训日志.docx', 'RESOURCE', 2, '/uploads/courses/1/4fedba5a-86fe-45f2-ae54-8887883df964_8209230308-刘麒源-学生实训日志.docx', NULL, NULL, NULL, '学生实习日志', 764, '{\"extension\":\"docx\",\"resourceType\":\"LINK\",\"chapterId\":1,\"splitStart\":3900,\"splitEnd\":4800,\"sourceType\":\"RESOURCE\",\"sourceId\":2}', '2026-07-14 12:24:32', '2026-07-14 12:24:32', 0);
INSERT INTO `course_knowledge_chunk` VALUES (9, 3, 1, 6, '关证明向实习队提出，两天以内由实习队负责人批准，超过两天的实习队同意后须请示二级单位教学与学工负责人共同批准方可准假。未经准假不参加实习者按旷课处理（医学生按医学实习有关规定执行）。\n四、实习期间，学生不允许离队外宿。\n五、自觉践行社会主义核心价值观，文明礼貌，团结友爱，谦逊好学，严禁酗酒闹事、打架斗殴及其他不文明行为。\n六、爱护公共财物，不得损坏或擅自动用校外基地(实习单位)的仪器设备、工具材料及其它物品。\n七、加强环保意识，爱护环境卫生，不浪费水电，培养勤俭节约的优良习惯。\n八、按照学校、二级单位规定以及实习教学大纲与指导教师的要求，认真做好实习日志，按时完成实习报告。\n附件2：\n中南大学学生实习安全规定\n一、严格遵守学校和校外基地(实习单位)的有关安全规章制度。\n二、实习队负责人为实习队安全第一责任人，各实习队要指定安全委员协助安全工作。二级单位主管安全工作负责人要定期检查实习队的安全情况。实习结束后，各实习队要向本单位汇报安全工作。\n三、实习前，请校外基地(实习单位)有关人员进行安全教育，熟悉安全防护要求和安全设施，熟悉安全生产操作规程。\n四、实习中，师生必须按校外基地(实习单位)规定统一穿戴防护用品，以确保人身安全。实习学生未经实习队或校外基地(实习单位)的允许不得擅自调换工作岗位。不得私自动用校外基地(实习单位)的生产仪器设备，不得擅自接触有毒有害物品。\n五、实习期间，遵守交通规则，防止交通事故；注意饮食卫生，防止食物中毒；注意防火、防盗、防抢。严禁到江、河、湖、海中游泳。\n六、实习期间如发生责任事故，要及时组织力量处理，并立即向校外基地(实习单位)和所在二级单位报告。\n七、实习期间凡违反国家、学校、校外基地(实习单位)的各项规定，造成个人人身安全和经济损失，由学生个人负责；造成国家、校外基地(实习单位)财产损失或生产事故的，视情节轻重，按照国家有关法律法规和学校、校外基地(实习单位)的有关规定追究当事学生的相关责任。\n附件3：\n中南大学学生实习保密规定\n一、严格遵守国家有关保密与知识产权保护的法律法规，严格遵守校外基地(实习单位)的保密制度。\n二、实习前，', '8209230308-刘麒源-学生实训日志.docx', 'RESOURCE', 2, '/uploads/courses/1/4fedba5a-86fe-45f2-ae54-8887883df964_8209230308-刘麒源-学生实训日志.docx', NULL, NULL, NULL, '学生实习日志', 775, '{\"extension\":\"docx\",\"resourceType\":\"LINK\",\"chapterId\":1,\"splitStart\":4680,\"splitEnd\":5580,\"sourceType\":\"RESOURCE\",\"sourceId\":2}', '2026-07-14 12:24:32', '2026-07-14 12:24:32', 0);
INSERT INTO `course_knowledge_chunk` VALUES (10, 3, 1, 7, '的，视情节轻重，按照国家有关法律法规和学校、校外基地(实习单位)的有关规定追究当事学生的相关责任。\n附件3：\n中南大学学生实习保密规定\n一、严格遵守国家有关保密与知识产权保护的法律法规，严格遵守校外基地(实习单位)的保密制度。\n二、实习前，二级单位必须对全体实习人员进行保密教育，学习保密法与有关保密规定。\n三、实习期间借阅、摘录、复印有关资料或拍摄实习现场实物、技术设备、生产工艺和其他有关内容，应事先征得校外基地(实习单位)的同意。\n四、一切涉密资料均应妥善保管，不得遗失。未经校外基地(实习单位)同意，涉密资料不得带出指定地点或转借他人。\n五、实习内容必须记在学校统一印制的《学生实习日志》上，不得将其撕毁或拆散使用。到保密部门实习时，实习结束后《学生实习日志》需交校外基地(实习单位)保密部门审查，然后由实习队带回交二级单位保存。\n六、学生实习期间取得的图片、原材料、产品样品、软件程序等涉密资料，回校后应统一交实习队负责人处理或上交所在二级单位，不得私自处理。\n七、不同专业、不同校外基地(实习单位)或不同实习任务的师生，都不得互相交流校外基地(实习单位)的涉密资料，不得互通保密情况，不得通过口头、书信、电子邮件、短信、微信等对外泄露机密，不得在不利于保密的场合谈论涉密内容。\n八、凡发生失密事故，实习队负责人必须立即组织调查，并将情况迅速报告给校外基地(实习单位)和学校、二级单位。\n附件4：\n中南大学学生自主实习申请审批表\n学院：                专业班级：                   填表日期：\n附件5：\n中南大学学生自主实习安全责任协议（学院保存联）\n经本人申请，学院批准，          专业      班学生             自行联系前往                 进行实习，实习时间从          至          止。\n学生本人保证实习期间自觉遵守国家法律法规和《中南大学本科实习教学管理办法》及《中南大学学生实习纪律》、《中南大学学生实习安全规定》、《中南大学学生实习保密规定》，遵守实习单位的各项规章制度，遵守社会公', '8209230308-刘麒源-学生实训日志.docx', 'RESOURCE', 2, '/uploads/courses/1/4fedba5a-86fe-45f2-ae54-8887883df964_8209230308-刘麒源-学生实训日志.docx', NULL, NULL, NULL, '学生实习日志', 686, '{\"extension\":\"docx\",\"resourceType\":\"LINK\",\"chapterId\":1,\"splitStart\":5460,\"splitEnd\":6360,\"sourceType\":\"RESOURCE\",\"sourceId\":2}', '2026-07-14 12:24:32', '2026-07-14 12:24:32', 0);
INSERT INTO `course_knowledge_chunk` VALUES (11, 3, 1, 8, '至          止。\n学生本人保证实习期间自觉遵守国家法律法规和《中南大学本科实习教学管理办法》及《中南大学学生实习纪律》、《中南大学学生实习安全规定》、《中南大学学生实习保密规定》，遵守实习单位的各项规章制度，遵守社会公德，自觉维护中南大学的声誉；谦虚好学，服从安排，注意安全，如自主实习期间出现安全、责任问题，由实习学生本人承担所有责任。\n中南大学                学院(盖章)        学生签字：            \n学院经办人签字：                         家长签字：            \n       年    月     日                   年    月     日\n中南大学学生自主实习安全责任协议(学生或家长保存联)\n经本人申请，学院批准，          专业      班学生             自行联系前往                    进行实习，实习时间从        至        止。\n学生本人保证实习期间自觉遵守国家法律法规和《中南大学本科实习教学管理办法》及《中南大学学生实习纪律》、《中南大学学生实习安全规定》、《中南大学学生实习保密规定》，遵守实习单位的各项规章制度，遵守社会公德，自觉维护中南大学的声誉；谦虚好学，服从安排，注意安全，如自主实习期间出现安全、责任问题，由实习学生本人承担所有责任。。\n中南大学                学院(盖章)        学生签字：            \n学院经办人签字：                         家长签字：            \n           年    月     日                     年    月     日\n中南大学关于进一步加强实践教学工作的通知(摘要)\n(中大教字[2014]10号)\n各二级单位：\n为贯彻落实《教育部等部门关于进一步加强高校实践育人工作的若干意见》（教思政[2012]1号）、《教育部关于全面提高高等教育质量的若干意', '8209230308-刘麒源-学生实训日志.docx', 'RESOURCE', 2, '/uploads/courses/1/4fedba5a-86fe-45f2-ae54-8887883df964_8209230308-刘麒源-学生实训日志.docx', NULL, NULL, NULL, '学生实习日志', 485, '{\"extension\":\"docx\",\"resourceType\":\"LINK\",\"chapterId\":1,\"splitStart\":6240,\"splitEnd\":7140,\"sourceType\":\"RESOURCE\",\"sourceId\":2}', '2026-07-14 12:24:32', '2026-07-14 12:24:32', 0);
INSERT INTO `course_knowledge_chunk` VALUES (12, 3, 1, 9, '日\n中南大学关于进一步加强实践教学工作的通知(摘要)\n(中大教字[2014]10号)\n各二级单位：\n为贯彻落实《教育部等部门关于进一步加强高校实践育人工作的若干意见》（教思政[2012]1号）、《教育部关于全面提高高等教育质量的若干意见》（教高[2012]4号）精神，切实提高实践教学质量与效果，全面提升本科人才培养质量，现结合新形势和我校实际，就有关事项通知如下。\n一、突出实践教学在人才培养中的重要地位和作用\n1.实践教学是高等学校人才培养不可缺少的重要内容和组成部分，是深化教育教学改革、巩固课堂理论教学、培养学生勇于探索的创新精神、增强学生善于解决问题的实践能力与创新创业能力、提高人才培养质量不可替代的重要途径和手段。实践教学包括实验、实习（实训）、毕业论文（设计)、课程设计（学年论文）、课外研学等环节。\n二、重视实践教学师资队伍建设\n5.建立健全校外实践教学兼职教师聘任和管理办法，建立兼职实践教师考评机制与考评办法，定期、集中培训校外实践教学兼职教师。\n三、深化实践教学改革和教材建设\n7.实践教学改革要以人为本，突出学生主体作用，注重教学过程，强调实践能力培养和系统的专业训练。建立以学生为中心的实践教学新模式，推进实践教学方法、内容、组织管理的全面改革与创新。支持和引导班级、社团等学生组织自主开展社会实践活动，发挥学生在实践教学中的自我教育、自我管理、自我服务作用。\n8.重视和加强实践教学教材建设工作。将实践教学教材统一纳入学校教材建设与管理，及时更新实践教材、讲义、资料、教学文档和教学内容。\n四、加强实践教学基地及条件建设\n10.加强实践教学基地建设。建设教学与科研紧密结合、学校与社会密切合作的实践教学基地。学校重点建设一批大型的高质量的综合性校企合作人才培养基地、实习实训基地、大学生创新创业基地、社会实践基地。实行基地校、院二级管理，确保各院系、各专业都有相对固定的实践基地。加强与实践基地的沟通协调，努力为学生各类实践活动提供便利和优惠条件。\n11.制订实践教学安全制度与突发事件处置预案，大力加强对学生的安全管理和安全保密教育，确保实践教学工作安全、高效、', '8209230308-刘麒源-学生实训日志.docx', 'RESOURCE', 2, '/uploads/courses/1/4fedba5a-86fe-45f2-ae54-8887883df964_8209230308-刘麒源-学生实训日志.docx', NULL, NULL, NULL, '学生实习日志', 779, '{\"extension\":\"docx\",\"resourceType\":\"LINK\",\"chapterId\":1,\"splitStart\":7020,\"splitEnd\":7920,\"sourceType\":\"RESOURCE\",\"sourceId\":2}', '2026-07-14 12:24:33', '2026-07-14 12:24:33', 0);
INSERT INTO `course_knowledge_chunk` VALUES (13, 3, 1, 10, '、院二级管理，确保各院系、各专业都有相对固定的实践基地。加强与实践基地的沟通协调，努力为学生各类实践活动提供便利和优惠条件。\n11.制订实践教学安全制度与突发事件处置预案，大力加强对学生的安全管理和安全保密教育，确保实践教学工作安全、高效、有序，保障师生人身安全。学院应统一给实习师生购买实习专项保险，解决学校与基地的后顾之忧，让家长、师生安心放心。\n五、严格经费管理，增加实践教学经费投入\n12.严格实践教学经费管理，确保实践教学经费与实践基地建设经费专款专用，切实保障各类实习时间和效果。各学院实习经费应单列管理，学院年度教学经费中划入实习经费单列帐户的比例不得低于60%。\n13.学校要加大实践教学经费投入，新增教学经费优先用于实践教学，支持实验室建设、实践教学基地建设、实践教学改革研究等。逐步提高各类实习经费的投入，鼓励和引导有条件的学院自筹经费补充实习教学。\n14.校、院两级要积极争取政府、企业、科研及社会力量支持，多渠道筹措、补充实践教学经费尤其是实习经费。不断改善学生实习待遇和条件，提高学生实习补贴标准，减轻学生实习负担。\n六、抓好实践教学质量监控与评价\n16.建立社会、教学督导专家、教师、学生等多方参与的实践教学评价与监控机制。充分发挥监控与评价的导向作用，加强实践教学有关信息的交流与反馈，不断提高实践教学水平和效果。\n七、强化实践教学各项工作\n18.加强实习组织和管理，认真落实实习计划，优化实习教学方案，确保学生实习时间，合理安排学生现场实习教学任务，不断提高实习教学效果。积极选聘基地专业技术人员担任兼职实习指导教师，鼓励带队教师与企业进行科技互动与交流。鼓励学生根据个性需求在保证实习教学要求和人身安全的前提下选择自主实习及长周期实习。\n中南大学\n2014年3月31日', '8209230308-刘麒源-学生实训日志.docx', 'RESOURCE', 2, '/uploads/courses/1/4fedba5a-86fe-45f2-ae54-8887883df964_8209230308-刘麒源-学生实训日志.docx', NULL, NULL, NULL, '学生实习日志', 665, '{\"extension\":\"docx\",\"resourceType\":\"LINK\",\"chapterId\":1,\"splitStart\":7800,\"splitEnd\":8551,\"sourceType\":\"RESOURCE\",\"sourceId\":2}', '2026-07-14 12:24:33', '2026-07-14 12:24:33', 0);
INSERT INTO `course_knowledge_chunk` VALUES (14, 7, 1, 0, '题目：C++ 中，以下哪个关键字用于声明虚函数？\n选项：[{\"label\": \"A\", \"content\": \"virtual\"}, {\"label\": \"B\", \"content\": \"override\"}, {\"label\": \"C\", \"content\": \"abstract\"}, {\"label\": \"D\", \"content\": \"static\"}]\n答案：A\n解析：virtual 是声明虚函数的关键字；override 用于显式标明重写；C++ 没有 abstract 关键字（纯虚函数用 =0 表示）；static 是静态成员关键字。\n知识点：C++程序设计', '题库解析 #6', 'QUESTION', 6, NULL, NULL, NULL, NULL, '题目 #6', 93, '{\"questionType\":\"SINGLE\",\"questionId\":6,\"knowledgePoint\":\"C++程序设计\",\"difficulty\":1,\"sourceType\":\"QUESTION\",\"sourceId\":6}', '2026-07-14 12:24:33', '2026-07-14 12:24:33', 0);
INSERT INTO `course_knowledge_chunk` VALUES (15, 8, 1, 0, '题目：以下关于引用的说法，正确的是？\n选项：[{\"label\": \"A\", \"content\": \"引用必须在定义时初始化\"}, {\"label\": \"B\", \"content\": \"引用可以重新绑定到另一个变量\"}, {\"label\": \"C\", \"content\": \"可以定义空引用\"}, {\"label\": \"D\", \"content\": \"引用和指针完全相同\"}]\n答案：A\n解析：引用定义时必须初始化且不能重新绑定；不存在空引用；引用是别名，指针是变量，两者不同。\n知识点：C++程序设计', '题库解析 #7', 'QUESTION', 7, NULL, NULL, NULL, NULL, '题目 #7', 120, '{\"questionType\":\"SINGLE\",\"questionId\":7,\"knowledgePoint\":\"C++程序设计\",\"difficulty\":1,\"sourceType\":\"QUESTION\",\"sourceId\":7}', '2026-07-14 12:24:33', '2026-07-14 12:24:33', 0);
INSERT INTO `course_knowledge_chunk` VALUES (16, 9, 1, 0, '题目：以下哪个 STL 容器底层由红黑树实现？\n选项：[{\"label\": \"A\", \"content\": \"vector\"}, {\"label\": \"B\", \"content\": \"deque\"}, {\"label\": \"C\", \"content\": \"map\"}, {\"label\": \"D\", \"content\": \"unordered_map\"}]\n答案：C\n解析：map/set 底层为红黑树（有序）；vector/deque 为动态数组；unordered_map 为哈希表。\n知识点：C++程序设计', '题库解析 #8', 'QUESTION', 8, NULL, NULL, NULL, NULL, '题目 #8', 72, '{\"questionType\":\"SINGLE\",\"questionId\":8,\"knowledgePoint\":\"C++程序设计\",\"difficulty\":2,\"sourceType\":\"QUESTION\",\"sourceId\":8}', '2026-07-14 12:24:34', '2026-07-14 12:24:34', 0);
INSERT INTO `course_knowledge_chunk` VALUES (17, 10, 1, 0, '题目：C++11 引入的 `auto` 关键字的作用是？\n选项：[{\"label\": \"A\", \"content\": \"声明静态变量\"}, {\"label\": \"B\", \"content\": \"自动推导变量类型\"}, {\"label\": \"C\", \"content\": \"定义自动存储期变量\"}, {\"label\": \"D\", \"content\": \"声明内联函数\"}]\n答案：B\n解析：C++11 中 auto 用于编译期自动类型推导；static 声明静态变量；auto 在 C 语言中的旧含义（自动存储期）已被弃用。\n知识点：C++程序设计', '题库解析 #9', 'QUESTION', 9, NULL, NULL, NULL, NULL, '题目 #9', 112, '{\"questionType\":\"SINGLE\",\"questionId\":9,\"knowledgePoint\":\"C++程序设计\",\"difficulty\":2,\"sourceType\":\"QUESTION\",\"sourceId\":9}', '2026-07-14 12:24:34', '2026-07-14 12:24:34', 0);
INSERT INTO `course_knowledge_chunk` VALUES (18, 11, 1, 0, '题目：以下哪个运算符不能被重载？\n选项：[{\"label\": \"A\", \"content\": \"+\"}, {\"label\": \"B\", \"content\": \"->\"}, {\"label\": \"C\", \"content\": \"::\"}, {\"label\": \"D\", \"content\": \"[]\"}]\n答案：C\n解析：::（作用域解析）、.（成员访问）、.*（成员指针访问）、?:（三目运算符）、sizeof、typeid 不能被重载。\n知识点：C++程序设计', '题库解析 #10', 'QUESTION', 10, NULL, NULL, NULL, NULL, '题目 #10', 68, '{\"questionType\":\"SINGLE\",\"questionId\":10,\"knowledgePoint\":\"C++程序设计\",\"difficulty\":3,\"sourceType\":\"QUESTION\",\"sourceId\":10}', '2026-07-14 12:24:34', '2026-07-14 12:24:34', 0);
INSERT INTO `course_knowledge_chunk` VALUES (19, 12, 1, 0, '题目：对于 `int* p = new int[10];`，正确的释放方式是？\n选项：[{\"label\": \"A\", \"content\": \"delete p;\"}, {\"label\": \"B\", \"content\": \"delete[] p;\"}, {\"label\": \"C\", \"content\": \"free(p);\"}, {\"label\": \"D\", \"content\": \"delete p[];\"}]\n答案：B\n解析：new[] 必须搭配 delete[]；delete 只释放单个对象；free 是 C 语言函数，不调用析构函数。混用会导致未定义行为。\n知识点：C++程序设计', '题库解析 #11', 'QUESTION', 11, NULL, NULL, NULL, NULL, '题目 #11', 90, '{\"questionType\":\"SINGLE\",\"questionId\":11,\"knowledgePoint\":\"C++程序设计\",\"difficulty\":1,\"sourceType\":\"QUESTION\",\"sourceId\":11}', '2026-07-14 12:24:35', '2026-07-14 12:24:35', 0);
INSERT INTO `course_knowledge_chunk` VALUES (20, 13, 1, 0, '题目：C++ 中实现运行时多态的机制是？\n选项：[{\"label\": \"A\", \"content\": \"函数重载\"}, {\"label\": \"B\", \"content\": \"模板\"}, {\"label\": \"C\", \"content\": \"虚函数和继承\"}, {\"label\": \"D\", \"content\": \"运算符重载\"}]\n答案：C\n解析：虚函数 + 基类指针/引用实现运行时多态（动态绑定）；函数重载和模板是编译时多态（静态绑定）。\n知识点：C++程序设计', '题库解析 #12', 'QUESTION', 12, NULL, NULL, NULL, NULL, '题目 #12', 96, '{\"questionType\":\"SINGLE\",\"questionId\":12,\"knowledgePoint\":\"C++程序设计\",\"difficulty\":2,\"sourceType\":\"QUESTION\",\"sourceId\":12}', '2026-07-14 12:24:35', '2026-07-14 12:24:35', 0);
INSERT INTO `course_knowledge_chunk` VALUES (21, 14, 1, 0, '题目：`std::move` 的作用是？\n选项：[{\"label\": \"A\", \"content\": \"移动对象的内存地址\"}, {\"label\": \"B\", \"content\": \"将左值转换为右值引用\"}, {\"label\": \"C\", \"content\": \"复制对象\"}, {\"label\": \"D\", \"content\": \"删除对象\"}]\n答案：B\n解析：std::move 本质是 static_cast<T&&>，将左值强制转为右值引用，从而触发移动构造/移动赋值，本身不移动任何数据。\n知识点：C++程序设计', '题库解析 #13', 'QUESTION', 13, NULL, NULL, NULL, NULL, '题目 #13', 102, '{\"questionType\":\"SINGLE\",\"questionId\":13,\"knowledgePoint\":\"C++程序设计\",\"difficulty\":3,\"sourceType\":\"QUESTION\",\"sourceId\":13}', '2026-07-14 12:24:35', '2026-07-14 12:24:35', 0);
INSERT INTO `course_knowledge_chunk` VALUES (22, 15, 1, 0, '题目：以下属于 C++ 面向对象特性的有？\n选项：[{\"label\": \"A\", \"content\": \"封装\"}, {\"label\": \"B\", \"content\": \"继承\"}, {\"label\": \"C\", \"content\": \"多态\"}, {\"label\": \"D\", \"content\": \"垃圾回收\"}]\n答案：A,B,C\n解析：面向对象三大特性：封装、继承、多态。C++ 没有内置垃圾回收机制（GC），需手动管理内存或使用智能指针。\n知识点：C++程序设计', '题库解析 #14', 'QUESTION', 14, NULL, NULL, NULL, NULL, '题目 #14', 94, '{\"questionType\":\"MULTI\",\"questionId\":14,\"knowledgePoint\":\"C++程序设计\",\"difficulty\":1,\"sourceType\":\"QUESTION\",\"sourceId\":14}', '2026-07-14 12:24:35', '2026-07-14 12:24:35', 0);
INSERT INTO `course_knowledge_chunk` VALUES (23, 16, 1, 0, '题目：以下属于 C++11 引入的智能指针是？\n选项：[{\"label\": \"A\", \"content\": \"std::unique_ptr\"}, {\"label\": \"B\", \"content\": \"std::shared_ptr\"}, {\"label\": \"C\", \"content\": \"std::auto_ptr\"}, {\"label\": \"D\", \"content\": \"std::weak_ptr\"}]\n答案：A,B,D\n解析：C++11 引入 unique_ptr、shared_ptr、weak_ptr；auto_ptr 在 C++98 中存在，C++11 已废弃，C++17 已移除。\n知识点：C++程序设计', '题库解析 #15', 'QUESTION', 15, NULL, NULL, NULL, NULL, '题目 #15', 85, '{\"questionType\":\"MULTI\",\"questionId\":15,\"knowledgePoint\":\"C++程序设计\",\"difficulty\":2,\"sourceType\":\"QUESTION\",\"sourceId\":15}', '2026-07-14 12:24:36', '2026-07-14 12:24:36', 0);
INSERT INTO `course_knowledge_chunk` VALUES (24, 17, 1, 0, '题目：以下关于 const 的说法正确的有？\n选项：[{\"label\": \"A\", \"content\": \"const 修饰的变量必须在定义时初始化\"}, {\"label\": \"B\", \"content\": \"const 成员函数不能修改任何成员变量\"}, {\"label\": \"C\", \"content\": \"const 对象只能调用 const 成员函数\"}, {\"label\": \"D\", \"content\": \"const 变量可以用作数组大小（C++）\"}]\n答案：A,B,C,D\n解析：以上均正确。A：const 变量定义时必须初始化；B：const 成员函数不能修改成员（mutable 除外）；C：const 对象不能调用非 const 函数；D：C++ 中 const 整数是编译期常量，可作为数组大小。\n知识点：C++程序设计', '题库解析 #16', 'QUESTION', 16, NULL, NULL, NULL, NULL, '题目 #16', 161, '{\"questionType\":\"MULTI\",\"questionId\":16,\"knowledgePoint\":\"C++程序设计\",\"difficulty\":2,\"sourceType\":\"QUESTION\",\"sourceId\":16}', '2026-07-14 12:24:36', '2026-07-14 12:24:36', 0);
INSERT INTO `course_knowledge_chunk` VALUES (25, 18, 1, 0, '题目：以下 STL 容器的插入操作时间复杂度为 O(1) 的有？\n选项：[{\"label\": \"A\", \"content\": \"vector 尾部插入\"}, {\"label\": \"B\", \"content\": \"list 任意位置插入\"}, {\"label\": \"C\", \"content\": \"map 插入\"}, {\"label\": \"D\", \"content\": \"unordered_map 平均插入\"}]\n答案：A,B,D\n解析：vector 尾部 push_back 均摊 O(1)；list 插入只需修改指针 O(1)；unordered_map 哈希表插入平均 O(1)；map 红黑树插入 O(logn)。\n知识点：C++程序设计', '题库解析 #17', 'QUESTION', 17, NULL, NULL, NULL, NULL, '题目 #17', 111, '{\"questionType\":\"MULTI\",\"questionId\":17,\"knowledgePoint\":\"C++程序设计\",\"difficulty\":3,\"sourceType\":\"QUESTION\",\"sourceId\":17}', '2026-07-14 12:24:36', '2026-07-14 12:24:36', 0);
INSERT INTO `course_knowledge_chunk` VALUES (26, 19, 1, 0, '题目：C++ 中，析构函数可以是虚函数，构造函数也可以是虚函数。\n选项：\n答案：0\n解析：析构函数可以是虚函数（且基类析构通常应声明为 virtual）；构造函数不能是虚函数，因为构造时虚表尚未建立。\n知识点：C++程序设计', '题库解析 #18', 'QUESTION', 18, NULL, NULL, NULL, NULL, '题目 #18', 83, '{\"questionType\":\"JUDGE\",\"questionId\":18,\"knowledgePoint\":\"C++程序设计\",\"difficulty\":2,\"sourceType\":\"QUESTION\",\"sourceId\":18}', '2026-07-14 12:24:36', '2026-07-14 12:24:36', 0);
INSERT INTO `course_knowledge_chunk` VALUES (27, 20, 1, 0, '题目：`std::vector` 在扩容时会将原有元素复制或移动到新的内存空间。\n选项：\n答案：1\n解析：vector 底层为连续内存，扩容时分配新空间，将旧元素移动/复制到新空间（优先使用移动语义），然后释放旧空间。\n知识点：C++程序设计', '题库解析 #19', 'QUESTION', 19, NULL, NULL, NULL, NULL, '题目 #19', 84, '{\"questionType\":\"JUDGE\",\"questionId\":19,\"knowledgePoint\":\"C++程序设计\",\"difficulty\":1,\"sourceType\":\"QUESTION\",\"sourceId\":19}', '2026-07-14 12:24:37', '2026-07-14 12:24:37', 0);
INSERT INTO `course_knowledge_chunk` VALUES (28, 21, 1, 0, '题目：C++ 中 `NULL` 和 `nullptr` 完全等价，可以互换使用。\n选项：\n答案：0\n解析：NULL 本质是整数 0（或 (void*)0），nullptr 是 std::nullptr_t 类型，类型安全。在重载决议中 nullptr 不会被误解析为整数，推荐使用 nullptr。\n知识点：C++程序设计', '题库解析 #20', 'QUESTION', 20, NULL, NULL, NULL, NULL, '题目 #20', 74, '{\"questionType\":\"JUDGE\",\"questionId\":20,\"knowledgePoint\":\"C++程序设计\",\"difficulty\":2,\"sourceType\":\"QUESTION\",\"sourceId\":20}', '2026-07-14 12:24:37', '2026-07-14 12:24:37', 0);
INSERT INTO `course_knowledge_chunk` VALUES (29, 22, 1, 0, '题目：模板函数的定义必须写在头文件中，不能分离到 .cpp 文件。\n选项：\n答案：1\n解析：模板在编译期实例化，编译器需要看到完整定义。若分离到 .cpp，其他翻译单元无法实例化（除非显式实例化）。通常模板定义放在头文件中。\n知识点：C++程序设计', '题库解析 #21', 'QUESTION', 21, NULL, NULL, NULL, NULL, '题目 #21', 95, '{\"questionType\":\"JUDGE\",\"questionId\":21,\"knowledgePoint\":\"C++程序设计\",\"difficulty\":3,\"sourceType\":\"QUESTION\",\"sourceId\":21}', '2026-07-14 12:24:38', '2026-07-14 12:24:38', 0);
INSERT INTO `course_knowledge_chunk` VALUES (30, 23, 1, 0, '题目：C++ 中，用于防止头文件重复包含的预处理指令是 ______。\n选项：\n答案：#pragma once 或 #ifndef / #define / #endif\n解析：两种方式均可：#pragma once（简洁，非标准但广泛支持）或传统的 #ifndef #define #endif 头文件保护宏。\n知识点：C++程序设计', '题库解析 #22', 'QUESTION', 22, NULL, NULL, NULL, NULL, '题目 #22', 73, '{\"questionType\":\"FILL\",\"questionId\":22,\"knowledgePoint\":\"C++程序设计\",\"difficulty\":1,\"sourceType\":\"QUESTION\",\"sourceId\":22}', '2026-07-14 12:24:38', '2026-07-14 12:24:38', 0);
INSERT INTO `course_knowledge_chunk` VALUES (31, 24, 1, 0, '题目：C++ 中，派生类构造函数初始化列表中调用基类构造函数的写法是：`Derived(int x) : ______ { }`（基类名为 Base）。\n选项：\n答案：Base(x)\n解析：派生类构造函数的初始化列表中通过 Base(x) 显式调用基类构造函数；若省略则默认调用基类无参构造函数。\n知识点：C++程序设计', '题库解析 #23', 'QUESTION', 23, NULL, NULL, NULL, NULL, '题目 #23', 97, '{\"questionType\":\"FILL\",\"questionId\":23,\"knowledgePoint\":\"C++程序设计\",\"difficulty\":2,\"sourceType\":\"QUESTION\",\"sourceId\":23}', '2026-07-14 12:24:38', '2026-07-14 12:24:38', 0);
INSERT INTO `course_knowledge_chunk` VALUES (32, 25, 1, 0, '题目：简述 C++ 中指针和引用的主要区别（至少 4 点）。\n选项：\n答案：1. 指针是变量，存储地址；引用是别名，不占额外空间（实现上可能是指针）。2. 引用必须初始化，指针可以为空。3. 引用不能重新绑定，指针可以修改指向。4. 有指针的指针（多级指针），无引用的引用。5. sizeof 引用得原类型大小，sizeof 指针得指针本身大小（通常 4/8 字节）。\n解析：指针存储地址可修改可空可多级；引用是别名必须初始化不可更改。两者各有适用场景，引用更安全简洁，指针更灵活。\n知识点：C++程序设计', '题库解析 #24', 'QUESTION', 24, NULL, NULL, NULL, NULL, '题目 #24', 185, '{\"questionType\":\"ESSAY\",\"questionId\":24,\"knowledgePoint\":\"C++程序设计\",\"difficulty\":2,\"sourceType\":\"QUESTION\",\"sourceId\":24}', '2026-07-14 12:24:39', '2026-07-14 12:24:39', 0);
INSERT INTO `course_knowledge_chunk` VALUES (33, 26, 1, 0, '题目：简述 C++ 中 `new/delete` 与 C 语言 `malloc/free` 的主要区别。\n选项：\n答案：1. new/delete 是 C++ 运算符，malloc/free 是 C 库函数。2. new 会自动计算类型大小并返回正确类型指针，malloc 需手动计算字节数并强转。3. new 会调用构造函数，delete 会调用析构函数；malloc/free 不调用构造/析构。4. new 分配失败抛 std::bad_alloc 异常，malloc 返回 NULL。5. new 可以被重载，malloc 不能。\n解析：核心区别：new/delete 是运算符，会调构造/析构函数，类型安全；malloc/free 是函数，仅分配/释放原始内存。不可混用。\n知识点：C++程序设计', '题库解析 #25', 'QUESTION', 25, NULL, NULL, NULL, NULL, '题目 #25', 172, '{\"questionType\":\"ESSAY\",\"questionId\":25,\"knowledgePoint\":\"C++程序设计\",\"difficulty\":2,\"sourceType\":\"QUESTION\",\"sourceId\":25}', '2026-07-14 12:24:39', '2026-07-14 12:24:39', 0);
INSERT INTO `course_knowledge_chunk` VALUES (34, 27, 1, 0, '题目：编写一个程序，输入两个整数 a 和 b，输出它们的和。\n\n输入格式：\n一行两个整数，用空格分隔。\n\n输出格式：\n一个整数，表示两数之和。\n选项：\n答案：\n解析：本题考察基本的输入输出和加法运算。注意使用 scanf/printf 进行格式化输入输出。\n知识点：C语言基础-输入输出', '题库解析 #37', 'QUESTION', 37, NULL, NULL, NULL, NULL, '题目 #37', 101, '{\"questionType\":\"CODE\",\"questionId\":37,\"knowledgePoint\":\"C语言基础-输入输出\",\"difficulty\":1,\"sourceType\":\"QUESTION\",\"sourceId\":37}', '2026-07-14 12:24:39', '2026-07-14 12:24:39', 0);
INSERT INTO `course_knowledge_chunk` VALUES (35, 28, 1, 0, '题目：编写一个程序，判断输入的正整数 n 是否为素数（质数）。如果是素数输出 \"YES\"，否则输出 \"NO\"。\n\n输入格式：\n一行一个正整数 n (1 <= n <= 10^6)。\n\n输出格式：\n\"YES\" 或 \"NO\"。\n选项：\n答案：\n解析：判断素数需要检查 2 到 sqrt(n) 之间的所有整数。可使用 i*i <= n 作为循环条件避免浮点运算。注意 1 不是素数。\n知识点：C++基础-循环与判断', '题库解析 #38', 'QUESTION', 38, NULL, NULL, NULL, NULL, '题目 #38', 121, '{\"questionType\":\"CODE\",\"questionId\":38,\"knowledgePoint\":\"C++基础-循环与判断\",\"difficulty\":3,\"sourceType\":\"QUESTION\",\"sourceId\":38}', '2026-07-14 12:24:39', '2026-07-14 12:24:39', 0);
INSERT INTO `course_knowledge_chunk` VALUES (36, 29, 1, 0, '题目：编写一个程序，输入一个整数 n，输出斐波那契数列的第 n 项（从 0 开始，F(0)=0, F(1)=1）。\n\n输入格式：\n一行一个整数 n (0 <= n <= 30)。\n\n输出格式：\n一个整数，表示 F(n) 的值。\n选项：\n答案：\n解析：可使用迭代法（循环）或递归法。考虑到 n <= 30，两种方法均可。推荐迭代法，时间复杂度 O(n)，空间复杂度 O(1)。\n知识点：Python基础-循环', '题库解析 #39', 'QUESTION', 39, NULL, NULL, NULL, NULL, '题目 #39', 121, '{\"questionType\":\"CODE\",\"questionId\":39,\"knowledgePoint\":\"Python基础-循环\",\"difficulty\":2,\"sourceType\":\"QUESTION\",\"sourceId\":39}', '2026-07-14 12:24:40', '2026-07-14 12:24:40', 0);
INSERT INTO `course_knowledge_chunk` VALUES (37, 30, 1, 0, '题目：编写一个冒泡排序程序。输入首先是一个整数 n（1 <= n <= 100），然后是 n 个待排序的整数。请将它们按从小到大的顺序输出，每个数之间用空格分隔。\n\n输入格式：\n第一行：整数 n\n第二行：n 个整数，空格分隔\n\n输出格式：\n一行，排序后的 n 个整数，空格分隔\n选项：\n答案：\n解析：冒泡排序的基本思想：重复遍历数组，比较相邻元素，如果顺序错误就交换。遍历 n-1 轮即可完成排序。时间复杂度 O(n²)。\n知识点：C++-排序算法', '题库解析 #40', 'QUESTION', 40, NULL, NULL, NULL, NULL, '题目 #40', 161, '{\"questionType\":\"CODE\",\"questionId\":40,\"knowledgePoint\":\"C++-排序算法\",\"difficulty\":4,\"sourceType\":\"QUESTION\",\"sourceId\":40}', '2026-07-14 12:24:40', '2026-07-14 12:24:40', 0);
INSERT INTO `course_knowledge_chunk` VALUES (38, 31, 1, 0, '问答标题：c++与rust哪个是更好的语言？\n问题内容：\n回复：rust\n回复：c++', 'c++与rust哪个是更好的语言？', 'QA', 1, NULL, NULL, NULL, NULL, 'c++与rust哪个是更好的语言？', 25, '{\"postId\":1,\"replyCount\":2,\"postType\":\"讨论\",\"sourceType\":\"QA\",\"sourceId\":1}', '2026-07-14 12:24:40', '2026-07-14 12:24:40', 0);

-- ----------------------------
-- Table structure for course_knowledge_document
-- ----------------------------
DROP TABLE IF EXISTS `course_knowledge_document`;
CREATE TABLE `course_knowledge_document`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文档标题',
  `source_type` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '来源类型：RESOURCE/ASSIGNMENT/QUESTION/QA/MANUAL',
  `source_id` bigint NULL DEFAULT NULL COMMENT '来源业务ID',
  `source_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '来源文件或页面地址',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'READY' COMMENT '处理状态：PROCESSING/READY/FAILED',
  `chunk_count` int NULL DEFAULT 0 COMMENT '分块数量',
  `char_count` int NULL DEFAULT 0 COMMENT '文本字符数',
  `error_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '失败原因',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_course`(`course_id` ASC) USING BTREE,
  INDEX `idx_source`(`source_type` ASC, `source_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 32 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '课程知识库文档' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of course_knowledge_document
-- ----------------------------
INSERT INTO `course_knowledge_document` VALUES (1, 2, '题库解析 #41', 'QUESTION', 41, NULL, 'READY', 1, 339, NULL, 2, '2026-07-14 12:24:17', '2026-07-14 12:24:17', 0);
INSERT INTO `course_knowledge_document` VALUES (2, 1, '屏幕录制 2026-07-11 144838.mp4', 'RESOURCE', 1, '/uploads/courses/1/6adc1c09-cd23-48d5-997e-f315250bf710_屏幕录制 2026-07-11 144838.mp4', 'READY', 1, 157, NULL, 2, '2026-07-14 12:24:30', '2026-07-14 12:24:30', 0);
INSERT INTO `course_knowledge_document` VALUES (3, 1, '8209230308-刘麒源-学生实训日志.docx', 'RESOURCE', 2, '/uploads/courses/1/4fedba5a-86fe-45f2-ae54-8887883df964_8209230308-刘麒源-学生实训日志.docx', 'READY', 11, 8551, NULL, 2, '2026-07-14 12:24:30', '2026-07-14 12:24:30', 0);
INSERT INTO `course_knowledge_document` VALUES (4, 1, '作业1', 'ASSIGNMENT', 1, NULL, 'READY', 0, 15, NULL, 2, '2026-07-14 12:24:33', '2026-07-14 12:24:33', 0);
INSERT INTO `course_knowledge_document` VALUES (5, 1, '作业2', 'ASSIGNMENT', 2, NULL, 'READY', 0, 18, NULL, 2, '2026-07-14 12:24:33', '2026-07-14 12:24:33', 0);
INSERT INTO `course_knowledge_document` VALUES (6, 1, '作业3', 'ASSIGNMENT', 3, NULL, 'READY', 0, 18, NULL, 2, '2026-07-14 12:24:33', '2026-07-14 12:24:33', 0);
INSERT INTO `course_knowledge_document` VALUES (7, 1, '题库解析 #6', 'QUESTION', 6, NULL, 'READY', 1, 288, NULL, 2, '2026-07-14 12:24:33', '2026-07-14 12:24:33', 0);
INSERT INTO `course_knowledge_document` VALUES (8, 1, '题库解析 #7', 'QUESTION', 7, NULL, 'READY', 1, 250, NULL, 2, '2026-07-14 12:24:33', '2026-07-14 12:24:33', 0);
INSERT INTO `course_knowledge_document` VALUES (9, 1, '题库解析 #8', 'QUESTION', 8, NULL, 'READY', 1, 256, NULL, 2, '2026-07-14 12:24:34', '2026-07-14 12:24:34', 0);
INSERT INTO `course_knowledge_document` VALUES (10, 1, '题库解析 #9', 'QUESTION', 9, NULL, 'READY', 1, 273, NULL, 2, '2026-07-14 12:24:34', '2026-07-14 12:24:34', 0);
INSERT INTO `course_knowledge_document` VALUES (11, 1, '题库解析 #10', 'QUESTION', 10, NULL, 'READY', 1, 231, NULL, 2, '2026-07-14 12:24:34', '2026-07-14 12:24:34', 0);
INSERT INTO `course_knowledge_document` VALUES (12, 1, '题库解析 #11', 'QUESTION', 11, NULL, 'READY', 1, 295, NULL, 2, '2026-07-14 12:24:35', '2026-07-14 12:24:35', 0);
INSERT INTO `course_knowledge_document` VALUES (13, 1, '题库解析 #12', 'QUESTION', 12, NULL, 'READY', 1, 233, NULL, 2, '2026-07-14 12:24:35', '2026-07-14 12:24:35', 0);
INSERT INTO `course_knowledge_document` VALUES (14, 1, '题库解析 #13', 'QUESTION', 13, NULL, 'READY', 1, 263, NULL, 2, '2026-07-14 12:24:35', '2026-07-14 12:24:35', 0);
INSERT INTO `course_knowledge_document` VALUES (15, 1, '题库解析 #14', 'QUESTION', 14, NULL, 'READY', 1, 236, NULL, 2, '2026-07-14 12:24:35', '2026-07-14 12:24:35', 0);
INSERT INTO `course_knowledge_document` VALUES (16, 1, '题库解析 #15', 'QUESTION', 15, NULL, 'READY', 1, 313, NULL, 2, '2026-07-14 12:24:36', '2026-07-14 12:24:36', 0);
INSERT INTO `course_knowledge_document` VALUES (17, 1, '题库解析 #16', 'QUESTION', 16, NULL, 'READY', 1, 373, NULL, 2, '2026-07-14 12:24:36', '2026-07-14 12:24:36', 0);
INSERT INTO `course_knowledge_document` VALUES (18, 1, '题库解析 #17', 'QUESTION', 17, NULL, 'READY', 1, 323, NULL, 2, '2026-07-14 12:24:36', '2026-07-14 12:24:36', 0);
INSERT INTO `course_knowledge_document` VALUES (19, 1, '题库解析 #18', 'QUESTION', 18, NULL, 'READY', 1, 112, NULL, 2, '2026-07-14 12:24:36', '2026-07-14 12:24:36', 0);
INSERT INTO `course_knowledge_document` VALUES (20, 1, '题库解析 #19', 'QUESTION', 19, NULL, 'READY', 1, 121, NULL, 2, '2026-07-14 12:24:37', '2026-07-14 12:24:37', 0);
INSERT INTO `course_knowledge_document` VALUES (21, 1, '题库解析 #20', 'QUESTION', 20, NULL, 'READY', 1, 161, NULL, 2, '2026-07-14 12:24:37', '2026-07-14 12:24:37', 0);
INSERT INTO `course_knowledge_document` VALUES (22, 1, '题库解析 #21', 'QUESTION', 21, NULL, 'READY', 1, 124, NULL, 2, '2026-07-14 12:24:38', '2026-07-14 12:24:38', 0);
INSERT INTO `course_knowledge_document` VALUES (23, 1, '题库解析 #22', 'QUESTION', 22, NULL, 'READY', 1, 167, NULL, 2, '2026-07-14 12:24:38', '2026-07-14 12:24:38', 0);
INSERT INTO `course_knowledge_document` VALUES (24, 1, '题库解析 #23', 'QUESTION', 23, NULL, 'READY', 1, 160, NULL, 2, '2026-07-14 12:24:38', '2026-07-14 12:24:38', 0);
INSERT INTO `course_knowledge_document` VALUES (25, 1, '题库解析 #24', 'QUESTION', 24, NULL, 'READY', 1, 253, NULL, 2, '2026-07-14 12:24:39', '2026-07-14 12:24:39', 0);
INSERT INTO `course_knowledge_document` VALUES (26, 1, '题库解析 #25', 'QUESTION', 25, NULL, 'READY', 1, 353, NULL, 2, '2026-07-14 12:24:39', '2026-07-14 12:24:39', 0);
INSERT INTO `course_knowledge_document` VALUES (27, 1, '题库解析 #37', 'QUESTION', 37, NULL, 'READY', 1, 143, NULL, 2, '2026-07-14 12:24:39', '2026-07-14 12:24:39', 0);
INSERT INTO `course_knowledge_document` VALUES (28, 1, '题库解析 #38', 'QUESTION', 38, NULL, 'READY', 1, 204, NULL, 2, '2026-07-14 12:24:39', '2026-07-14 12:24:39', 0);
INSERT INTO `course_knowledge_document` VALUES (29, 1, '题库解析 #39', 'QUESTION', 39, NULL, 'READY', 1, 203, NULL, 2, '2026-07-14 12:24:40', '2026-07-14 12:24:40', 0);
INSERT INTO `course_knowledge_document` VALUES (30, 1, '题库解析 #40', 'QUESTION', 40, NULL, 'READY', 1, 224, NULL, 2, '2026-07-14 12:24:40', '2026-07-14 12:24:40', 0);
INSERT INTO `course_knowledge_document` VALUES (31, 1, 'c++与rust哪个是更好的语言？', 'QA', 1, NULL, 'READY', 1, 45, NULL, 2, '2026-07-14 12:24:40', '2026-07-14 12:24:40', 0);

-- ----------------------------
-- Table structure for course_knowledge_qa_history
-- ----------------------------
DROP TABLE IF EXISTS `course_knowledge_qa_history`;
CREATE TABLE `course_knowledge_qa_history`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `user_id` bigint NOT NULL COMMENT '提问用户ID',
  `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提问内容',
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '回答内容',
  `references_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '参考来源JSON',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_course_id`(`course_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '课程知识库问答历史' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for course_resource
-- ----------------------------
DROP TABLE IF EXISTS `course_resource`;
CREATE TABLE `course_resource`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `chapter_id` bigint NULL DEFAULT NULL COMMENT '所属章节ID',
  `resource_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '资源名称',
  `resource_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '资源类型：VIDEO / PDF / PPT / LINK',
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件URL',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序号',
  `file_size` bigint NULL DEFAULT 0 COMMENT '文件大小（字节）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_course_id`(`course_id` ASC) USING BTREE,
  INDEX `idx_chapter_id`(`chapter_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '课程资源表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of course_resource
-- ----------------------------
INSERT INTO `course_resource` VALUES (1, 1, 1, '屏幕录制 2026-07-11 144838.mp4', 'VIDEO', '/uploads/courses/1/6adc1c09-cd23-48d5-997e-f315250bf710_屏幕录制 2026-07-11 144838.mp4', 0, 5421757, NULL, NULL, 0);
INSERT INTO `course_resource` VALUES (2, 1, 1, '8209230308-刘麒源-学生实训日志.docx', 'LINK', '/uploads/courses/1/4fedba5a-86fe-45f2-ae54-8887883df964_8209230308-刘麒源-学生实训日志.docx', 0, 84838, '2026-07-11 17:13:18', '2026-07-11 17:13:18', 0);

-- ----------------------------
-- Table structure for course_student
-- ----------------------------
DROP TABLE IF EXISTS `course_student`;
CREATE TABLE `course_student`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `student_id` bigint NOT NULL COMMENT '学生用户ID（关联sys_user）',
  `enrolled_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '选课时间',
  `progress` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '学习进度百分比',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_course_student`(`course_id` ASC, `student_id` ASC) USING BTREE,
  INDEX `idx_student_id`(`student_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '选课关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of course_student
-- ----------------------------
INSERT INTO `course_student` VALUES (1, 1, 3, '2026-07-09 20:34:50', 100.00, NULL, '2026-07-11 16:48:22', 0);
INSERT INTO `course_student` VALUES (2, 2, 3, '2026-07-11 20:16:41', 30.00, '2026-07-11 20:16:41', '2026-07-11 20:16:41', 0);

-- ----------------------------
-- Table structure for evaluation
-- ----------------------------
DROP TABLE IF EXISTS `evaluation`;
CREATE TABLE `evaluation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint NULL DEFAULT NULL COMMENT '课程ID',
  `evaluation_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评价名称',
  `evaluation_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评价类型：教师评学 / 学生评教',
  `start_date` date NULL DEFAULT NULL COMMENT '开始日期',
  `end_date` date NULL DEFAULT NULL COMMENT '结束日期',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '未开始' COMMENT '状态：未开始 / 进行中 / 已结束',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_course_id`(`course_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评价表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of evaluation
-- ----------------------------

-- ----------------------------
-- Table structure for evaluation_item
-- ----------------------------
DROP TABLE IF EXISTS `evaluation_item`;
CREATE TABLE `evaluation_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `evaluation_id` bigint NOT NULL COMMENT '评价表ID',
  `item_content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评价指标内容',
  `item_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'RADIO' COMMENT '指标类型：RADIO（单选评分）/ TEXT（文本）',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序号',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_evaluation_id`(`evaluation_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评价指标项表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of evaluation_item
-- ----------------------------

-- ----------------------------
-- Table structure for evaluation_result
-- ----------------------------
DROP TABLE IF EXISTS `evaluation_result`;
CREATE TABLE `evaluation_result`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `evaluation_id` bigint NOT NULL COMMENT '评价表ID',
  `evaluator_id` bigint NOT NULL COMMENT '评价人用户ID',
  `target_user_id` bigint NOT NULL COMMENT '被评价人用户ID',
  `item_id` bigint NOT NULL COMMENT '评价指标项ID',
  `score` int NULL DEFAULT NULL COMMENT '评分（1-5星）',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '文本评价内容',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_evaluation_id`(`evaluation_id` ASC) USING BTREE,
  INDEX `idx_evaluator_id`(`evaluator_id` ASC) USING BTREE,
  INDEX `idx_eval_evaluator_deleted`(`evaluation_id` ASC, `evaluator_id` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评价结果表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of evaluation_result
-- ----------------------------

-- ----------------------------
-- Table structure for exam
-- ----------------------------
DROP TABLE IF EXISTS `exam`;
CREATE TABLE `exam`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `exam_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '考试名称',
  `paper_id` bigint NOT NULL COMMENT '试卷ID',
  `course_id` bigint NULL DEFAULT NULL COMMENT '课程ID',
  `start_time` datetime NOT NULL COMMENT '考试开始时间',
  `end_time` datetime NOT NULL COMMENT '考试结束时间',
  `duration` int NULL DEFAULT 120 COMMENT '考试时长（分钟）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '未开始' COMMENT '状态：未开始 / 进行中 / 已结束',
  `allow_retry` tinyint(1) NULL DEFAULT 0 COMMENT '是否允许重考',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_paper_id`(`paper_id` ASC) USING BTREE,
  INDEX `idx_course_id`(`course_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_course_status_deleted`(`course_id` ASC, `status` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '考试安排表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exam
-- ----------------------------
INSERT INTO `exam` VALUES (1, 'c++期中测试', 2, 1, '2026-07-10 16:00:00', '2026-07-24 16:00:00', 90, '未开始', 0, '2026-07-11 16:46:57', '2026-07-11 16:46:57', 0);
INSERT INTO `exam` VALUES (2, 'c++编程测试', 4, 1, '2026-07-10 16:00:00', '2026-07-30 16:00:00', 90, '未开始', 1, '2026-07-11 19:42:48', '2026-07-11 20:03:29', 0);
INSERT INTO `exam` VALUES (3, 'sql', 5, 2, '2026-07-10 16:00:00', '2026-07-30 16:00:00', 90, '未开始', 1, '2026-07-11 20:17:45', '2026-07-11 20:17:45', 0);

-- ----------------------------
-- Table structure for exam_paper
-- ----------------------------
DROP TABLE IF EXISTS `exam_paper`;
CREATE TABLE `exam_paper`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `paper_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '试卷名称',
  `course_id` bigint NULL DEFAULT NULL COMMENT '所属课程ID',
  `total_score` decimal(6, 2) NULL DEFAULT 100.00 COMMENT '试卷总分',
  `duration` int NULL DEFAULT 120 COMMENT '考试时长（分钟）',
  `pass_score` decimal(6, 2) NULL DEFAULT 60.00 COMMENT '及格分数线',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '草稿' COMMENT '状态：草稿 / 已发布',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建教师ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_course_id`(`course_id` ASC) USING BTREE,
  INDEX `idx_create_by`(`create_by` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '试卷表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exam_paper
-- ----------------------------
INSERT INTO `exam_paper` VALUES (1, 'C++程序设计基础试卷', 1, 60.00, 90, 30.00, '已发布', 2, '2026-07-11 16:32:17', '2026-07-11 16:32:17', 0);
INSERT INTO `exam_paper` VALUES (2, 'C++程序设计中等难度试卷', 1, 100.00, 90, 60.00, '已发布', 2, '2026-07-11 16:33:07', '2026-07-11 16:33:07', 0);
INSERT INTO `exam_paper` VALUES (3, 'C++程序设计综合试卷', 1, 70.00, 90, 60.00, '已发布', 2, '2026-07-11 16:39:31', '2026-07-11 16:39:31', 0);
INSERT INTO `exam_paper` VALUES (4, '编程测试', 1, 30.00, 90, 15.00, '已发布', 2, '2026-07-11 19:41:56', '2026-07-11 19:41:56', 0);
INSERT INTO `exam_paper` VALUES (5, '数据库测试试卷', 2, 10.00, 90, 6.00, '已发布', 2, '2026-07-11 20:17:14', '2026-07-11 20:17:14', 0);

-- ----------------------------
-- Table structure for exam_paper_question
-- ----------------------------
DROP TABLE IF EXISTS `exam_paper_question`;
CREATE TABLE `exam_paper_question`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `paper_id` bigint NOT NULL COMMENT '试卷ID',
  `question_id` bigint NOT NULL COMMENT '试题ID',
  `question_score` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '该题分值',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序号',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_paper_id`(`paper_id` ASC) USING BTREE,
  INDEX `idx_question_id`(`question_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '试卷-试题关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exam_paper_question
-- ----------------------------
INSERT INTO `exam_paper_question` VALUES (1, 1, 6, 20.00, 1, '2026-07-11 16:32:17', '2026-07-11 16:32:17', 0);
INSERT INTO `exam_paper_question` VALUES (2, 1, 7, 10.00, 2, '2026-07-11 16:32:17', '2026-07-11 16:32:17', 0);
INSERT INTO `exam_paper_question` VALUES (3, 1, 11, 15.00, 3, '2026-07-11 16:32:17', '2026-07-11 16:32:17', 0);
INSERT INTO `exam_paper_question` VALUES (4, 1, 14, 10.00, 4, '2026-07-11 16:32:17', '2026-07-11 16:32:17', 0);
INSERT INTO `exam_paper_question` VALUES (5, 1, 22, 5.00, 5, '2026-07-11 16:32:17', '2026-07-11 16:32:17', 0);
INSERT INTO `exam_paper_question` VALUES (6, 2, 10, 25.00, 1, '2026-07-11 16:33:07', '2026-07-11 16:33:07', 0);
INSERT INTO `exam_paper_question` VALUES (7, 2, 13, 25.00, 2, '2026-07-11 16:33:07', '2026-07-11 16:33:07', 0);
INSERT INTO `exam_paper_question` VALUES (8, 2, 17, 30.00, 3, '2026-07-11 16:33:07', '2026-07-11 16:33:07', 0);
INSERT INTO `exam_paper_question` VALUES (9, 2, 21, 20.00, 4, '2026-07-11 16:33:07', '2026-07-11 16:33:07', 0);
INSERT INTO `exam_paper_question` VALUES (10, 3, 7, 5.00, 1, '2026-07-11 16:39:31', '2026-07-11 16:39:31', 0);
INSERT INTO `exam_paper_question` VALUES (11, 3, 8, 5.00, 2, '2026-07-11 16:39:31', '2026-07-11 16:39:31', 0);
INSERT INTO `exam_paper_question` VALUES (12, 3, 14, 8.00, 3, '2026-07-11 16:39:31', '2026-07-11 16:39:31', 0);
INSERT INTO `exam_paper_question` VALUES (13, 3, 16, 8.00, 4, '2026-07-11 16:39:31', '2026-07-11 16:39:31', 0);
INSERT INTO `exam_paper_question` VALUES (14, 3, 19, 5.00, 5, '2026-07-11 16:39:31', '2026-07-11 16:39:31', 0);
INSERT INTO `exam_paper_question` VALUES (15, 3, 22, 5.00, 6, '2026-07-11 16:39:31', '2026-07-11 16:39:31', 0);
INSERT INTO `exam_paper_question` VALUES (16, 3, 23, 5.00, 7, '2026-07-11 16:39:31', '2026-07-11 16:39:31', 0);
INSERT INTO `exam_paper_question` VALUES (17, 3, 24, 12.00, 8, '2026-07-11 16:39:31', '2026-07-11 16:39:31', 0);
INSERT INTO `exam_paper_question` VALUES (18, 3, 25, 12.00, 9, '2026-07-11 16:39:31', '2026-07-11 16:39:31', 0);
INSERT INTO `exam_paper_question` VALUES (19, 3, 10, 5.00, 10, '2026-07-11 16:39:31', '2026-07-11 16:39:31', 0);
INSERT INTO `exam_paper_question` VALUES (20, 4, 37, 10.00, 1, '2026-07-11 19:41:56', '2026-07-11 19:41:56', 0);
INSERT INTO `exam_paper_question` VALUES (21, 4, 40, 20.00, 2, '2026-07-11 19:41:56', '2026-07-11 19:41:56', 0);
INSERT INTO `exam_paper_question` VALUES (22, 5, 41, 10.00, 1, '2026-07-11 20:17:14', '2026-07-11 20:17:14', 0);

-- ----------------------------
-- Table structure for exam_record
-- ----------------------------
DROP TABLE IF EXISTS `exam_record`;
CREATE TABLE `exam_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `exam_id` bigint NOT NULL COMMENT '考试ID',
  `student_id` bigint NOT NULL COMMENT '学生用户ID',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始作答时间',
  `submit_time` datetime NULL DEFAULT NULL COMMENT '提交时间',
  `total_score` decimal(6, 2) NULL DEFAULT NULL COMMENT '总分',
  `pass_status` tinyint(1) NULL DEFAULT 0 COMMENT '是否及格：0否 / 1是',
  `is_submitted` tinyint(1) NULL DEFAULT 0 COMMENT '是否已提交：0否 / 1是',
  `cheat_count` int NULL DEFAULT 0 COMMENT '切屏次数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_exam_student`(`exam_id` ASC, `student_id` ASC) USING BTREE,
  INDEX `idx_student_id`(`student_id` ASC) USING BTREE,
  INDEX `idx_exam_submitted_deleted`(`exam_id` ASC, `is_submitted` ASC, `is_deleted` ASC) USING BTREE,
  INDEX `idx_student_submitted_deleted`(`student_id` ASC, `is_submitted` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '考试记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exam_record
-- ----------------------------
INSERT INTO `exam_record` VALUES (1, 1, 3, '2026-07-11 16:47:17', '2026-07-11 16:48:22', 25.00, 0, 1, 0, '2026-07-11 16:47:17', '2026-07-11 16:47:17', 0);
INSERT INTO `exam_record` VALUES (2, 2, 3, '2026-07-11 20:03:34', '2026-07-11 20:04:52', 2.00, 0, 1, 0, '2026-07-11 19:43:04', '2026-07-11 19:43:04', 0);
INSERT INTO `exam_record` VALUES (3, 3, 3, '2026-07-13 09:02:57', '2026-07-13 09:03:32', 10.00, 1, 1, 0, '2026-07-11 20:18:02', '2026-07-11 20:18:02', 0);

-- ----------------------------
-- Table structure for exam_record_answer
-- ----------------------------
DROP TABLE IF EXISTS `exam_record_answer`;
CREATE TABLE `exam_record_answer`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `record_id` bigint NOT NULL COMMENT '考试记录ID',
  `question_id` bigint NOT NULL COMMENT '试题ID',
  `student_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '学生答案',
  `is_correct` tinyint(1) NULL DEFAULT NULL COMMENT '客观题是否正确：0错误 / 1正确',
  `score` decimal(5, 2) NULL DEFAULT NULL COMMENT '得分',
  `marked_by` bigint NULL DEFAULT NULL COMMENT '批阅教师ID（主观题）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_record_id`(`record_id` ASC) USING BTREE,
  INDEX `idx_question_id`(`question_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '答题详情表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exam_record_answer
-- ----------------------------
INSERT INTO `exam_record_answer` VALUES (1, 1, 10, 'B', 0, 0.00, NULL, '2026-07-11 16:48:22', '2026-07-11 16:48:22', 0);
INSERT INTO `exam_record_answer` VALUES (2, 1, 13, 'B', 1, 25.00, NULL, '2026-07-11 16:48:22', '2026-07-11 16:48:22', 0);
INSERT INTO `exam_record_answer` VALUES (3, 1, 17, 'A,D', 0, 0.00, NULL, '2026-07-11 16:48:22', '2026-07-11 16:48:22', 0);
INSERT INTO `exam_record_answer` VALUES (4, 1, 21, '0', 0, 0.00, NULL, '2026-07-11 16:48:22', '2026-07-11 16:48:22', 0);
INSERT INTO `exam_record_answer` VALUES (5, 2, 37, '#include <stdio.h>\n\nint main() {\n    int a, b;\n    scanf(\"%d %d\", &a, &b);\n    printf(\"%d\\n\", a + b);\n    return 0;\n}', 1, 10.00, NULL, '2026-07-11 19:52:57', '2026-07-11 20:03:33', 1);
INSERT INTO `exam_record_answer` VALUES (6, 2, 40, '#include <iostream>\n#include <vector>\nusing namespace std;\n\nint main() {\n    int n;\n    cin >> n;\n    vector<int> arr(n);\n    for (int i = 0; i < n; i++) cin >> arr[i];\n    // 在此实现冒泡排序\n    return 0;\n}', 0, 0.00, NULL, '2026-07-11 19:53:00', '2026-07-11 20:03:33', 1);
INSERT INTO `exam_record_answer` VALUES (7, 2, 37, '#include <stdio.h>\n\nint main() {\n    int a, b;\n    scanf(\"%d %d\", &a, &b);\n    printf(\"%d\\n\", a - b);\n    return 0;\n}', 0, 2.00, NULL, '2026-07-11 20:04:55', '2026-07-11 20:04:55', 0);
INSERT INTO `exam_record_answer` VALUES (8, 2, 40, '#include <iostream>\n#include <vector>\nusing namespace std;\n\nint main() {\n    int n;\n    cin >> n;\n    vector<int> arr(n);\n    for (int i = 0; i < n; i++) cin >> arr[i];\n    // 在此实现冒泡排序\n    return 0;\n}', 0, 0.00, NULL, '2026-07-11 20:04:58', '2026-07-11 20:04:58', 0);
INSERT INTO `exam_record_answer` VALUES (9, 3, 41, '-- 创建表并插入数据\nCREATE TABLE student (id INT, name TEXT, score INT);\nINSERT INTO student VALUES (1, \'张三\', 85), (2, \'李四\', 92), (3, \'王五\', 78), (4, \'赵六\', 88), (5, \'孙七\', 95);\n\n-- 在此编写查询语句\nSELECT name || \'|\' || score FROM student WHERE score >= 85 ORDER BY score DESC;', 1, 10.00, NULL, '2026-07-11 20:18:20', '2026-07-13 09:02:56', 1);
INSERT INTO `exam_record_answer` VALUES (10, 3, 41, '-- 创建表并插入数据\nCREATE TABLE student (id INT, name TEXT, score INT);\nINSERT INTO student VALUES (1, \'张三\', 85), (2, \'李四\', 92), (3, \'王五\', 78), (4, \'赵六\', 88), (5, \'孙七\', 95);\n\n-- 在此编写查询语句\nSELECT name || \'|\' || score FROM student WHERE score >= 85 ORDER BY score DESC;', 1, 10.00, NULL, '2026-07-13 09:03:33', '2026-07-13 09:03:33', 0);

-- ----------------------------
-- Table structure for grade_course
-- ----------------------------
DROP TABLE IF EXISTS `grade_course`;
CREATE TABLE `grade_course`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `student_id` bigint NOT NULL COMMENT '学生用户ID',
  `exam_avg_score` decimal(5, 2) NULL DEFAULT NULL COMMENT '考试平均分',
  `assignment_avg_score` decimal(5, 2) NULL DEFAULT NULL COMMENT '作业平均分',
  `total_score` decimal(5, 2) NULL DEFAULT NULL COMMENT '总评成绩',
  `grade_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '等级：优秀 / 良好 / 中等 / 及格 / 不及格',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_course_student`(`course_id` ASC, `student_id` ASC) USING BTREE,
  INDEX `idx_student_id`(`student_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '课程总评成绩表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of grade_course
-- ----------------------------
INSERT INTO `grade_course` VALUES (1, 1, 3, 15.80, 83.30, 42.80, '不及格', NULL, '2026-07-11 16:03:37', 0);
INSERT INTO `grade_course` VALUES (2, 2, 3, 100.00, NULL, 100.00, '优秀', '2026-07-11 20:18:20', '2026-07-11 20:18:20', 0);

-- ----------------------------
-- Table structure for knowledge_article
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_article`;
CREATE TABLE `knowledge_article`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文章标题',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '文章内容',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分类',
  `tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签（逗号分隔）',
  `create_by` bigint NULL DEFAULT NULL COMMENT '发布人用户ID',
  `view_count` int NULL DEFAULT 0 COMMENT '浏览次数',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '发布' COMMENT '状态：草稿 / 发布',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_create_by`(`create_by` ASC) USING BTREE,
  FULLTEXT INDEX `ft_title_content`(`title`, `content`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识库文章表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of knowledge_article
-- ----------------------------

-- ----------------------------
-- Table structure for learning_note
-- ----------------------------
DROP TABLE IF EXISTS `learning_note`;
CREATE TABLE `learning_note`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` bigint NOT NULL COMMENT '学生用户ID',
  `course_id` bigint NULL DEFAULT NULL COMMENT '课程ID',
  `chapter_id` bigint NULL DEFAULT NULL COMMENT '章节ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '笔记标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '笔记内容',
  `video_timestamp` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联视频时间点（如02:35）',
  `is_public` tinyint(1) NULL DEFAULT 0 COMMENT '是否公开分享',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_student_id`(`student_id` ASC) USING BTREE,
  INDEX `idx_course_id`(`course_id` ASC) USING BTREE,
  INDEX `idx_chapter_deleted`(`chapter_id` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学习笔记表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of learning_note
-- ----------------------------
INSERT INTO `learning_note` VALUES (1, 3, 2, NULL, '数据库系统学习笔记', '<h3>c++课程笔记一</h3>\n<p>语法：c++是编译型语言,int a = b;</p>\n<ol>\n <li>\n  <p style=\"text-align: left;\"><span style=\"font-size: 18px;\">呼呼</span></p></li>\n <li>\n  <p></p><img src=\"http://localhost:8080/uploads/common/eedc03b9-f112-4833-978a-95757f8f3453_p1.png\"></li>\n</ol>\n<p></p>', NULL, 0, '2026-07-13 12:40:06', '2026-07-13 13:17:49', 0);

-- ----------------------------
-- Table structure for notification
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '通知内容',
  `sender_id` bigint NULL DEFAULT NULL COMMENT '发送者用户ID',
  `send_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `notification_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '系统' COMMENT '通知类型：系统 / 课程 / 考试 / 作业 / 公告',
  `course_id` bigint NULL DEFAULT NULL COMMENT '关联课程ID(仅公告类型使用)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sender_id`(`sender_id` ASC) USING BTREE,
  INDEX `idx_send_time`(`send_time` ASC) USING BTREE,
  INDEX `idx_notification_type`(`notification_type` ASC) USING BTREE,
  INDEX `idx_course_id`(`course_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '通知表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notification
-- ----------------------------
INSERT INTO `notification` VALUES (1, '私信', '魔丸！', 3, '2026-07-09 20:36:53', '私信', NULL, NULL, '2026-07-10 21:17:59', 1);
INSERT INTO `notification` VALUES (2, '私信', '你才是魔丸', 2, '2026-07-09 20:38:01', '私信', NULL, NULL, '2026-07-10 21:17:59', 1);
INSERT INTO `notification` VALUES (3, '私信', '啦啦啦', 3, '2026-07-10 16:13:13', '私信', NULL, NULL, '2026-07-10 21:17:59', 1);
INSERT INTO `notification` VALUES (4, '私信', '和', 2, '2026-07-10 16:17:20', '私信', NULL, NULL, '2026-07-10 21:17:59', 1);
INSERT INTO `notification` VALUES (5, '私信', '老师', 3, '2026-07-10 16:20:26', '私信', NULL, NULL, '2026-07-10 21:17:59', 1);
INSERT INTO `notification` VALUES (6, '私信', '可开可', 2, '2026-07-10 16:20:44', '私信', NULL, NULL, '2026-07-10 21:17:59', 1);
INSERT INTO `notification` VALUES (7, '私信', 'kkk', 3, '2026-07-10 16:37:29', '私信', NULL, NULL, '2026-07-10 21:17:59', 1);
INSERT INTO `notification` VALUES (8, '私信', '啦啦啦', 2, '2026-07-10 16:42:24', '私信', NULL, NULL, '2026-07-10 21:17:59', 1);
INSERT INTO `notification` VALUES (9, '完成作业！', '速度', 2, '2026-07-11 15:10:51', '公告', 1, NULL, '2026-07-11 15:11:15', 1);
INSERT INTO `notification` VALUES (10, '完成作业', '进阶卷\n', 2, '2026-07-11 15:12:36', '公告', 1, NULL, NULL, 0);
INSERT INTO `notification` VALUES (11, '新考试：c++期中测试', '课程《c++》发布了考试，请在规定时间内参加。', 2, '2026-07-11 16:46:57', '考试', 1, '2026-07-11 16:46:57', '2026-07-11 16:46:57', 0);
INSERT INTO `notification` VALUES (12, '新考试：c++编程测试', '课程《c++》发布了考试，请在规定时间内参加。', 2, '2026-07-11 19:42:48', '考试', 1, '2026-07-11 19:42:48', '2026-07-11 19:42:48', 0);
INSERT INTO `notification` VALUES (13, '新考试：sql', '课程《数据库系统》发布了考试，请在规定时间内参加。', 2, '2026-07-11 20:17:45', '考试', 2, '2026-07-11 20:17:45', '2026-07-11 20:17:45', 0);
INSERT INTO `notification` VALUES (14, '作业已批改：作业3', '你的作业《作业3》已完成批改，得分：60.0。', 2, '2026-07-13 13:56:30', '作业', 1, '2026-07-13 13:56:30', '2026-07-13 13:56:30', 0);

-- ----------------------------
-- Table structure for notification_user
-- ----------------------------
DROP TABLE IF EXISTS `notification_user`;
CREATE TABLE `notification_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `notification_id` bigint NOT NULL COMMENT '通知ID',
  `user_id` bigint NOT NULL COMMENT '接收用户ID',
  `is_read` tinyint(1) NULL DEFAULT 0 COMMENT '是否已读',
  `read_time` datetime NULL DEFAULT NULL COMMENT '阅读时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_read`(`user_id` ASC, `is_read` ASC) USING BTREE,
  INDEX `idx_notification_id`(`notification_id` ASC) USING BTREE,
  INDEX `idx_user_read_deleted`(`user_id` ASC, `is_read` ASC, `is_deleted` ASC) USING BTREE,
  INDEX `idx_notif_user_deleted`(`notification_id` ASC, `user_id` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户通知状态表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notification_user
-- ----------------------------
INSERT INTO `notification_user` VALUES (1, 1, 2, 1, '2026-07-09 20:37:39', NULL, '2026-07-10 21:17:59', 1);
INSERT INTO `notification_user` VALUES (2, 2, 3, 1, '2026-07-10 15:05:36', NULL, '2026-07-10 21:17:59', 1);
INSERT INTO `notification_user` VALUES (3, 3, 2, 1, '2026-07-10 16:16:18', NULL, '2026-07-10 21:17:59', 1);
INSERT INTO `notification_user` VALUES (4, 4, 3, 1, '2026-07-10 16:20:09', NULL, '2026-07-10 21:17:59', 1);
INSERT INTO `notification_user` VALUES (5, 5, 2, 1, '2026-07-10 16:20:31', NULL, '2026-07-10 21:17:59', 1);
INSERT INTO `notification_user` VALUES (6, 6, 3, 1, '2026-07-10 16:20:45', NULL, '2026-07-10 21:17:59', 1);
INSERT INTO `notification_user` VALUES (7, 7, 2, 1, '2026-07-10 16:42:18', NULL, '2026-07-10 21:17:59', 1);
INSERT INTO `notification_user` VALUES (8, 8, 3, 1, '2026-07-10 16:43:07', NULL, '2026-07-10 21:17:59', 1);
INSERT INTO `notification_user` VALUES (9, 9, 3, 1, '2026-07-11 15:11:07', NULL, '2026-07-11 15:11:15', 1);
INSERT INTO `notification_user` VALUES (10, 10, 3, 1, '2026-07-11 15:12:54', NULL, NULL, 0);
INSERT INTO `notification_user` VALUES (11, 11, 3, 1, '2026-07-11 17:04:32', '2026-07-11 16:46:57', '2026-07-11 16:46:57', 0);
INSERT INTO `notification_user` VALUES (12, 12, 3, 0, NULL, '2026-07-11 19:42:48', '2026-07-11 19:42:48', 0);
INSERT INTO `notification_user` VALUES (13, 13, 3, 0, NULL, '2026-07-11 20:17:45', '2026-07-11 20:17:45', 0);
INSERT INTO `notification_user` VALUES (14, 14, 3, 0, NULL, '2026-07-13 13:56:30', '2026-07-13 13:56:30', 0);

-- ----------------------------
-- Table structure for operation_log
-- ----------------------------
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `operator_id` bigint NULL DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作人姓名',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '角色编码',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务模块',
  `action` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作动作',
  `target_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '目标类型',
  `target_id` bigint NULL DEFAULT NULL COMMENT '目标ID',
  `target_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '目标名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作描述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_operator_id`(`operator_id` ASC) USING BTREE,
  INDEX `idx_module`(`module` ASC) USING BTREE,
  INDEX `idx_action`(`action` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_module_action_deleted`(`module` ASC, `action` ASC, `is_deleted` ASC) USING BTREE,
  INDEX `idx_target_deleted`(`target_type` ASC, `target_id` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '平台操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of operation_log
-- ----------------------------
INSERT INTO `operation_log` VALUES (1, 2, '周士博', 'ROLE_TEACHER', '课程管理', '上传资源', 'COURSE_RESOURCE', 1, '屏幕录制 2026-07-11 144838.mp4', '上传课程资源', NULL, 0);
INSERT INTO `operation_log` VALUES (2, 2, '周士博', 'ROLE_TEACHER', '考试管理', '创建试卷', 'PAPER', 1, 'C++程序设计基础试卷', '创建考试试卷', '2026-07-11 16:32:17', 0);
INSERT INTO `operation_log` VALUES (3, 2, '周士博', 'ROLE_TEACHER', '考试管理', '创建试卷', 'PAPER', 2, 'C++程序设计中等难度试卷', '创建考试试卷', '2026-07-11 16:33:07', 0);
INSERT INTO `operation_log` VALUES (4, 2, '周士博', 'ROLE_TEACHER', '考试管理', '创建试卷', 'PAPER', 3, 'C++程序设计综合试卷', '创建考试试卷', '2026-07-11 16:39:31', 0);
INSERT INTO `operation_log` VALUES (5, 2, '周士博', 'ROLE_TEACHER', '考试管理', '发布考试', 'EXAM', 1, 'c++期中测试', '发布课程考试', '2026-07-11 16:46:57', 0);
INSERT INTO `operation_log` VALUES (6, 2, '周士博', 'ROLE_TEACHER', '课程管理', '上传资源', 'COURSE_RESOURCE', 2, '8209230308-刘麒源-学生实训日志.docx', '上传课程资源', '2026-07-11 17:13:18', 0);
INSERT INTO `operation_log` VALUES (7, 2, '周士博', 'ROLE_TEACHER', '考试管理', '创建试卷', 'PAPER', 4, '编程测试', '创建考试试卷', '2026-07-11 19:41:56', 0);
INSERT INTO `operation_log` VALUES (8, 2, '周士博', 'ROLE_TEACHER', '考试管理', '发布考试', 'EXAM', 2, 'c++编程测试', '发布课程考试', '2026-07-11 19:42:48', 0);
INSERT INTO `operation_log` VALUES (9, 2, '周士博', 'ROLE_TEACHER', '考试管理', '编辑考试', 'EXAM', 2, 'c++编程测试', '更新考试安排', '2026-07-11 20:03:29', 0);
INSERT INTO `operation_log` VALUES (10, 2, '周士博', 'ROLE_TEACHER', '课程管理', '创建课程', 'COURSE', 2, '数据库系统', '创建课程', '2026-07-11 20:16:30', 0);
INSERT INTO `operation_log` VALUES (11, 2, '周士博', 'ROLE_TEACHER', '考试管理', '创建试卷', 'PAPER', 5, '数据库测试试卷', '创建考试试卷', '2026-07-11 20:17:14', 0);
INSERT INTO `operation_log` VALUES (12, 2, '周士博', 'ROLE_TEACHER', '考试管理', '发布考试', 'EXAM', 3, 'sql', '发布课程考试', '2026-07-11 20:17:45', 0);
INSERT INTO `operation_log` VALUES (13, 2, '周士博', 'ROLE_TEACHER', '作业管理', '批改作业', 'SUBMIT', 3, '作业3', '批改学生作业并给出成绩', '2026-07-13 13:56:30', 0);

-- ----------------------------
-- Table structure for participation_point
-- ----------------------------
DROP TABLE IF EXISTS `participation_point`;
CREATE TABLE `participation_point`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `student_id` bigint NOT NULL COMMENT '学生用户ID',
  `teacher_id` bigint NULL DEFAULT NULL COMMENT '操作教师ID',
  `points` int NULL DEFAULT 1 COMMENT '积分',
  `source_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '随机点名' COMMENT '来源类型',
  `reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '积分说明',
  `event_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_student_id`(`student_id` ASC) USING BTREE,
  INDEX `idx_course_id`(`course_id` ASC) USING BTREE,
  INDEX `idx_event_time`(`event_time` ASC) USING BTREE,
  INDEX `idx_teacher_deleted`(`teacher_id` ASC, `is_deleted` ASC) USING BTREE,
  INDEX `idx_course_student_deleted`(`course_id` ASC, `student_id` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '课堂参与积分流水表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of participation_point
-- ----------------------------
INSERT INTO `participation_point` VALUES (1, 1, 3, 2, 3, '随机点名', '课堂互动', '2026-07-10 14:55:57', NULL, NULL, 0);
INSERT INTO `participation_point` VALUES (2, 1, 3, 2, -1, '随机点名', '课堂互动', '2026-07-10 14:56:03', NULL, NULL, 0);
INSERT INTO `participation_point` VALUES (3, 1, 3, 2, 1, '课堂签到', '签到: 测试签到', '2026-07-10 17:40:22', NULL, NULL, 0);
INSERT INTO `participation_point` VALUES (4, 1, 3, 2, 1, '课堂签到', '签到: 测试二维码签到', '2026-07-10 17:41:50', NULL, NULL, 0);
INSERT INTO `participation_point` VALUES (5, 1, 3, 2, 1, '课堂签到', '签到: 测试位置签到', '2026-07-10 17:42:34', NULL, NULL, 0);
INSERT INTO `participation_point` VALUES (6, 2, 3, 2, 1, '课堂签到', '签到: 数据库签到', '2026-07-13 13:23:47', '2026-07-13 13:23:47', '2026-07-13 13:23:47', 0);
INSERT INTO `participation_point` VALUES (7, 2, 3, 2, 1, '课堂签到', '签到: 时间签到', '2026-07-13 13:31:12', '2026-07-13 13:31:12', '2026-07-13 13:31:12', 0);
INSERT INTO `participation_point` VALUES (8, 2, 3, 2, 1, '课堂签到', '签到: 测试Ui', '2026-07-13 13:38:12', '2026-07-13 13:38:12', '2026-07-13 13:38:12', 0);

-- ----------------------------
-- Table structure for question
-- ----------------------------
DROP TABLE IF EXISTS `question`;
CREATE TABLE `question`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `question_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '题型：SINGLE / MULTI / JUDGE / FILL / ESSAY',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '题目内容',
  `options` json NULL COMMENT '选项（JSON格式）',
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '正确答案',
  `analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '解析',
  `score` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '默认分值',
  `difficulty` int NULL DEFAULT 3 COMMENT '难度：1-5',
  `knowledge_point` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '知识点',
  `course_id` bigint NULL DEFAULT NULL COMMENT '所属课程ID',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建教师ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  `programming_config` json NULL COMMENT '编程题配置：{language,templateCode,timeLimit,memoryLimit,testCases,hiddenTestCases,scoringMode}',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_question_type`(`question_type` ASC) USING BTREE,
  INDEX `idx_course_id`(`course_id` ASC) USING BTREE,
  INDEX `idx_difficulty`(`difficulty` ASC) USING BTREE,
  INDEX `idx_knowledge_point`(`knowledge_point` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 42 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '试题库表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of question
-- ----------------------------
INSERT INTO `question` VALUES (1, 'SINGLE', '测试', '[\"啊\", \"是\", \"的\", \"发\"]', '是', '', 5.00, 3, '', 1, 2, NULL, '2026-07-11 16:25:08', 1, NULL);
INSERT INTO `question` VALUES (2, 'SINGLE', '单选2', '[\"安安\", \"的\", \"丰富\", \"各个\"]', '的', '', 5.00, 3, '', 1, 2, '2026-07-11 16:07:45', '2026-07-11 16:25:07', 1, NULL);
INSERT INTO `question` VALUES (3, 'JUDGE', '地方烦烦烦', NULL, '对', '', 5.00, 3, '', 1, 2, '2026-07-11 16:07:54', '2026-07-11 16:25:05', 1, NULL);
INSERT INTO `question` VALUES (4, 'ESSAY', 'c++的基本语法', NULL, '八八八八', '', 10.00, 3, '', 1, 2, '2026-07-11 16:08:14', '2026-07-11 16:25:04', 1, NULL);
INSERT INTO `question` VALUES (5, 'SINGLE', '帆帆帆帆', '[\"丰富\", \"各个\", \"黑盒\", \"简洁\"]', '丰富', '', 5.00, 3, '', 1, 2, '2026-07-11 16:08:26', '2026-07-11 16:25:02', 1, NULL);
INSERT INTO `question` VALUES (6, 'SINGLE', 'C++ 中，以下哪个关键字用于声明虚函数？', '[{\"label\": \"A\", \"content\": \"virtual\"}, {\"label\": \"B\", \"content\": \"override\"}, {\"label\": \"C\", \"content\": \"abstract\"}, {\"label\": \"D\", \"content\": \"static\"}]', 'A', 'virtual 是声明虚函数的关键字；override 用于显式标明重写；C++ 没有 abstract 关键字（纯虚函数用 =0 表示）；static 是静态成员关键字。', 5.00, 1, 'C++程序设计', 1, 2, '2026-07-11 16:29:30', '2026-07-11 16:29:30', 0, NULL);
INSERT INTO `question` VALUES (7, 'SINGLE', '以下关于引用的说法，正确的是？', '[{\"label\": \"A\", \"content\": \"引用必须在定义时初始化\"}, {\"label\": \"B\", \"content\": \"引用可以重新绑定到另一个变量\"}, {\"label\": \"C\", \"content\": \"可以定义空引用\"}, {\"label\": \"D\", \"content\": \"引用和指针完全相同\"}]', 'A', '引用定义时必须初始化且不能重新绑定；不存在空引用；引用是别名，指针是变量，两者不同。', 5.00, 1, 'C++程序设计', 1, 2, '2026-07-11 16:29:30', '2026-07-11 16:29:30', 0, NULL);
INSERT INTO `question` VALUES (8, 'SINGLE', '以下哪个 STL 容器底层由红黑树实现？', '[{\"label\": \"A\", \"content\": \"vector\"}, {\"label\": \"B\", \"content\": \"deque\"}, {\"label\": \"C\", \"content\": \"map\"}, {\"label\": \"D\", \"content\": \"unordered_map\"}]', 'C', 'map/set 底层为红黑树（有序）；vector/deque 为动态数组；unordered_map 为哈希表。', 5.00, 2, 'C++程序设计', 1, 2, '2026-07-11 16:29:30', '2026-07-11 16:29:30', 0, NULL);
INSERT INTO `question` VALUES (9, 'SINGLE', 'C++11 引入的 `auto` 关键字的作用是？', '[{\"label\": \"A\", \"content\": \"声明静态变量\"}, {\"label\": \"B\", \"content\": \"自动推导变量类型\"}, {\"label\": \"C\", \"content\": \"定义自动存储期变量\"}, {\"label\": \"D\", \"content\": \"声明内联函数\"}]', 'B', 'C++11 中 auto 用于编译期自动类型推导；static 声明静态变量；auto 在 C 语言中的旧含义（自动存储期）已被弃用。', 5.00, 2, 'C++程序设计', 1, 2, '2026-07-11 16:29:30', '2026-07-11 16:29:30', 0, NULL);
INSERT INTO `question` VALUES (10, 'SINGLE', '以下哪个运算符不能被重载？', '[{\"label\": \"A\", \"content\": \"+\"}, {\"label\": \"B\", \"content\": \"->\"}, {\"label\": \"C\", \"content\": \"::\"}, {\"label\": \"D\", \"content\": \"[]\"}]', 'C', '::（作用域解析）、.（成员访问）、.*（成员指针访问）、?:（三目运算符）、sizeof、typeid 不能被重载。', 5.00, 3, 'C++程序设计', 1, 2, '2026-07-11 16:29:30', '2026-07-11 16:29:30', 0, NULL);
INSERT INTO `question` VALUES (11, 'SINGLE', '对于 `int* p = new int[10];`，正确的释放方式是？', '[{\"label\": \"A\", \"content\": \"delete p;\"}, {\"label\": \"B\", \"content\": \"delete[] p;\"}, {\"label\": \"C\", \"content\": \"free(p);\"}, {\"label\": \"D\", \"content\": \"delete p[];\"}]', 'B', 'new[] 必须搭配 delete[]；delete 只释放单个对象；free 是 C 语言函数，不调用析构函数。混用会导致未定义行为。', 5.00, 1, 'C++程序设计', 1, 2, '2026-07-11 16:29:30', '2026-07-11 16:29:30', 0, NULL);
INSERT INTO `question` VALUES (12, 'SINGLE', 'C++ 中实现运行时多态的机制是？', '[{\"label\": \"A\", \"content\": \"函数重载\"}, {\"label\": \"B\", \"content\": \"模板\"}, {\"label\": \"C\", \"content\": \"虚函数和继承\"}, {\"label\": \"D\", \"content\": \"运算符重载\"}]', 'C', '虚函数 + 基类指针/引用实现运行时多态（动态绑定）；函数重载和模板是编译时多态（静态绑定）。', 5.00, 2, 'C++程序设计', 1, 2, '2026-07-11 16:29:30', '2026-07-11 16:29:30', 0, NULL);
INSERT INTO `question` VALUES (13, 'SINGLE', '`std::move` 的作用是？', '[{\"label\": \"A\", \"content\": \"移动对象的内存地址\"}, {\"label\": \"B\", \"content\": \"将左值转换为右值引用\"}, {\"label\": \"C\", \"content\": \"复制对象\"}, {\"label\": \"D\", \"content\": \"删除对象\"}]', 'B', 'std::move 本质是 static_cast<T&&>，将左值强制转为右值引用，从而触发移动构造/移动赋值，本身不移动任何数据。', 5.00, 3, 'C++程序设计', 1, 2, '2026-07-11 16:29:30', '2026-07-11 16:29:30', 0, NULL);
INSERT INTO `question` VALUES (14, 'MULTI', '以下属于 C++ 面向对象特性的有？', '[{\"label\": \"A\", \"content\": \"封装\"}, {\"label\": \"B\", \"content\": \"继承\"}, {\"label\": \"C\", \"content\": \"多态\"}, {\"label\": \"D\", \"content\": \"垃圾回收\"}]', 'A,B,C', '面向对象三大特性：封装、继承、多态。C++ 没有内置垃圾回收机制（GC），需手动管理内存或使用智能指针。', 8.00, 1, 'C++程序设计', 1, 2, '2026-07-11 16:29:30', '2026-07-11 16:29:30', 0, NULL);
INSERT INTO `question` VALUES (15, 'MULTI', '以下属于 C++11 引入的智能指针是？', '[{\"label\": \"A\", \"content\": \"std::unique_ptr\"}, {\"label\": \"B\", \"content\": \"std::shared_ptr\"}, {\"label\": \"C\", \"content\": \"std::auto_ptr\"}, {\"label\": \"D\", \"content\": \"std::weak_ptr\"}]', 'A,B,D', 'C++11 引入 unique_ptr、shared_ptr、weak_ptr；auto_ptr 在 C++98 中存在，C++11 已废弃，C++17 已移除。', 8.00, 2, 'C++程序设计', 1, 2, '2026-07-11 16:29:30', '2026-07-11 16:29:30', 0, NULL);
INSERT INTO `question` VALUES (16, 'MULTI', '以下关于 const 的说法正确的有？', '[{\"label\": \"A\", \"content\": \"const 修饰的变量必须在定义时初始化\"}, {\"label\": \"B\", \"content\": \"const 成员函数不能修改任何成员变量\"}, {\"label\": \"C\", \"content\": \"const 对象只能调用 const 成员函数\"}, {\"label\": \"D\", \"content\": \"const 变量可以用作数组大小（C++）\"}]', 'A,B,C,D', '以上均正确。A：const 变量定义时必须初始化；B：const 成员函数不能修改成员（mutable 除外）；C：const 对象不能调用非 const 函数；D：C++ 中 const 整数是编译期常量，可作为数组大小。', 8.00, 2, 'C++程序设计', 1, 2, '2026-07-11 16:29:30', '2026-07-11 16:29:30', 0, NULL);
INSERT INTO `question` VALUES (17, 'MULTI', '以下 STL 容器的插入操作时间复杂度为 O(1) 的有？', '[{\"label\": \"A\", \"content\": \"vector 尾部插入\"}, {\"label\": \"B\", \"content\": \"list 任意位置插入\"}, {\"label\": \"C\", \"content\": \"map 插入\"}, {\"label\": \"D\", \"content\": \"unordered_map 平均插入\"}]', 'A,B,D', 'vector 尾部 push_back 均摊 O(1)；list 插入只需修改指针 O(1)；unordered_map 哈希表插入平均 O(1)；map 红黑树插入 O(logn)。', 8.00, 3, 'C++程序设计', 1, 2, '2026-07-11 16:29:30', '2026-07-11 16:29:30', 0, NULL);
INSERT INTO `question` VALUES (18, 'JUDGE', 'C++ 中，析构函数可以是虚函数，构造函数也可以是虚函数。', NULL, '0', '析构函数可以是虚函数（且基类析构通常应声明为 virtual）；构造函数不能是虚函数，因为构造时虚表尚未建立。', 5.00, 2, 'C++程序设计', 1, 2, '2026-07-11 16:29:30', '2026-07-11 16:29:30', 0, NULL);
INSERT INTO `question` VALUES (19, 'JUDGE', '`std::vector` 在扩容时会将原有元素复制或移动到新的内存空间。', NULL, '1', 'vector 底层为连续内存，扩容时分配新空间，将旧元素移动/复制到新空间（优先使用移动语义），然后释放旧空间。', 5.00, 1, 'C++程序设计', 1, 2, '2026-07-11 16:29:30', '2026-07-11 16:29:30', 0, NULL);
INSERT INTO `question` VALUES (20, 'JUDGE', 'C++ 中 `NULL` 和 `nullptr` 完全等价，可以互换使用。', NULL, '0', 'NULL 本质是整数 0（或 (void*)0），nullptr 是 std::nullptr_t 类型，类型安全。在重载决议中 nullptr 不会被误解析为整数，推荐使用 nullptr。', 5.00, 2, 'C++程序设计', 1, 2, '2026-07-11 16:29:30', '2026-07-11 16:29:30', 0, NULL);
INSERT INTO `question` VALUES (21, 'JUDGE', '模板函数的定义必须写在头文件中，不能分离到 .cpp 文件。', NULL, '1', '模板在编译期实例化，编译器需要看到完整定义。若分离到 .cpp，其他翻译单元无法实例化（除非显式实例化）。通常模板定义放在头文件中。', 5.00, 3, 'C++程序设计', 1, 2, '2026-07-11 16:29:30', '2026-07-11 16:29:30', 0, NULL);
INSERT INTO `question` VALUES (22, 'FILL', 'C++ 中，用于防止头文件重复包含的预处理指令是 ______。', NULL, '#pragma once 或 #ifndef / #define / #endif', '两种方式均可：#pragma once（简洁，非标准但广泛支持）或传统的 #ifndef #define #endif 头文件保护宏。', 5.00, 1, 'C++程序设计', 1, 2, '2026-07-11 16:29:30', '2026-07-11 16:29:30', 0, NULL);
INSERT INTO `question` VALUES (23, 'FILL', 'C++ 中，派生类构造函数初始化列表中调用基类构造函数的写法是：`Derived(int x) : ______ { }`（基类名为 Base）。', NULL, 'Base(x)', '派生类构造函数的初始化列表中通过 Base(x) 显式调用基类构造函数；若省略则默认调用基类无参构造函数。', 5.00, 2, 'C++程序设计', 1, 2, '2026-07-11 16:29:30', '2026-07-11 16:29:30', 0, NULL);
INSERT INTO `question` VALUES (24, 'ESSAY', '简述 C++ 中指针和引用的主要区别（至少 4 点）。', NULL, '1. 指针是变量，存储地址；引用是别名，不占额外空间（实现上可能是指针）。2. 引用必须初始化，指针可以为空。3. 引用不能重新绑定，指针可以修改指向。4. 有指针的指针（多级指针），无引用的引用。5. sizeof 引用得原类型大小，sizeof 指针得指针本身大小（通常 4/8 字节）。', '指针存储地址可修改可空可多级；引用是别名必须初始化不可更改。两者各有适用场景，引用更安全简洁，指针更灵活。', 12.00, 2, 'C++程序设计', 1, 2, '2026-07-11 16:29:30', '2026-07-11 16:29:30', 0, NULL);
INSERT INTO `question` VALUES (25, 'ESSAY', '简述 C++ 中 `new/delete` 与 C 语言 `malloc/free` 的主要区别。', NULL, '1. new/delete 是 C++ 运算符，malloc/free 是 C 库函数。2. new 会自动计算类型大小并返回正确类型指针，malloc 需手动计算字节数并强转。3. new 会调用构造函数，delete 会调用析构函数；malloc/free 不调用构造/析构。4. new 分配失败抛 std::bad_alloc 异常，malloc 返回 NULL。5. new 可以被重载，malloc 不能。', '核心区别：new/delete 是运算符，会调构造/析构函数，类型安全；malloc/free 是函数，仅分配/释放原始内存。不可混用。', 12.00, 2, 'C++程序设计', 1, 2, '2026-07-11 16:29:30', '2026-07-11 16:29:30', 0, NULL);
INSERT INTO `question` VALUES (37, 'CODE', '编写一个程序，输入两个整数 a 和 b，输出它们的和。\n\n输入格式：\n一行两个整数，用空格分隔。\n\n输出格式：\n一个整数，表示两数之和。', NULL, '', '本题考察基本的输入输出和加法运算。注意使用 scanf/printf 进行格式化输入输出。', 10.00, 1, 'C语言基础-输入输出', 1, NULL, '2026-07-11 19:34:01', '2026-07-11 19:41:08', 0, '{\"language\": \"c\", \"testCases\": [{\"input\": \"1 2\", \"expectedOutput\": \"3\"}, {\"input\": \"10 20\", \"expectedOutput\": \"30\"}, {\"input\": \"-5 8\", \"expectedOutput\": \"3\"}], \"timeLimit\": 3, \"memoryLimit\": 128, \"scoringMode\": \"perCase\", \"templateCode\": \"#include <stdio.h>\\n\\nint main() {\\n    int a, b;\\n    scanf(\\\"%d %d\\\", &a, &b);\\n    printf(\\\"%d\\\\n\\\", a + b);\\n    return 0;\\n}\", \"hiddenTestCases\": [{\"input\": \"100 200\", \"expectedOutput\": \"300\"}, {\"input\": \"0 0\", \"expectedOutput\": \"0\"}]}');
INSERT INTO `question` VALUES (38, 'CODE', '编写一个程序，判断输入的正整数 n 是否为素数（质数）。如果是素数输出 \"YES\"，否则输出 \"NO\"。\n\n输入格式：\n一行一个正整数 n (1 <= n <= 10^6)。\n\n输出格式：\n\"YES\" 或 \"NO\"。', NULL, '', '判断素数需要检查 2 到 sqrt(n) 之间的所有整数。可使用 i*i <= n 作为循环条件避免浮点运算。注意 1 不是素数。', 15.00, 3, 'C++基础-循环与判断', 1, NULL, '2026-07-11 19:34:01', '2026-07-11 19:41:12', 0, '{\"language\": \"cpp\", \"testCases\": [{\"input\": \"7\", \"expectedOutput\": \"YES\"}, {\"input\": \"10\", \"expectedOutput\": \"NO\"}, {\"input\": \"2\", \"expectedOutput\": \"YES\"}], \"timeLimit\": 5, \"memoryLimit\": 256, \"scoringMode\": \"perCase\", \"templateCode\": \"#include <iostream>\\n#include <cmath>\\nusing namespace std;\\n\\nint main() {\\n    int n;\\n    cin >> n;\\n    // 在此编写代码\\n    return 0;\\n}\", \"hiddenTestCases\": [{\"input\": \"1\", \"expectedOutput\": \"NO\"}, {\"input\": \"999983\", \"expectedOutput\": \"YES\"}, {\"input\": \"1000000\", \"expectedOutput\": \"NO\"}]}');
INSERT INTO `question` VALUES (39, 'CODE', '编写一个程序，输入一个整数 n，输出斐波那契数列的第 n 项（从 0 开始，F(0)=0, F(1)=1）。\n\n输入格式：\n一行一个整数 n (0 <= n <= 30)。\n\n输出格式：\n一个整数，表示 F(n) 的值。', NULL, '', '可使用迭代法（循环）或递归法。考虑到 n <= 30，两种方法均可。推荐迭代法，时间复杂度 O(n)，空间复杂度 O(1)。', 10.00, 2, 'Python基础-循环', 1, NULL, '2026-07-11 19:34:01', '2026-07-11 19:41:16', 0, '{\"language\": \"python\", \"testCases\": [{\"input\": \"0\", \"expectedOutput\": \"0\"}, {\"input\": \"5\", \"expectedOutput\": \"5\"}, {\"input\": \"10\", \"expectedOutput\": \"55\"}], \"timeLimit\": 3, \"memoryLimit\": 128, \"scoringMode\": \"perCase\", \"templateCode\": \"def fibonacci(n):\\n    # 在此编写代码\\n    pass\\n\\nn = int(input())\\nprint(fibonacci(n))\", \"hiddenTestCases\": [{\"input\": \"20\", \"expectedOutput\": \"6765\"}, {\"input\": \"30\", \"expectedOutput\": \"832040\"}]}');
INSERT INTO `question` VALUES (40, 'CODE', '编写一个冒泡排序程序。输入首先是一个整数 n（1 <= n <= 100），然后是 n 个待排序的整数。请将它们按从小到大的顺序输出，每个数之间用空格分隔。\n\n输入格式：\n第一行：整数 n\n第二行：n 个整数，空格分隔\n\n输出格式：\n一行，排序后的 n 个整数，空格分隔', NULL, '', '冒泡排序的基本思想：重复遍历数组，比较相邻元素，如果顺序错误就交换。遍历 n-1 轮即可完成排序。时间复杂度 O(n²)。', 20.00, 4, 'C++-排序算法', 1, NULL, '2026-07-11 19:34:01', '2026-07-11 19:41:21', 0, '{\"language\": \"cpp\", \"testCases\": [{\"input\": \"5\\n3 1 4 1 5\", \"expectedOutput\": \"1 1 3 4 5\"}, {\"input\": \"3\\n10 5 8\", \"expectedOutput\": \"5 8 10\"}], \"timeLimit\": 5, \"memoryLimit\": 256, \"scoringMode\": \"perCase\", \"templateCode\": \"#include <iostream>\\n#include <vector>\\nusing namespace std;\\n\\nint main() {\\n    int n;\\n    cin >> n;\\n    vector<int> arr(n);\\n    for (int i = 0; i < n; i++) cin >> arr[i];\\n    // 在此实现冒泡排序\\n    return 0;\\n}\", \"hiddenTestCases\": [{\"input\": \"6\\n1 2 3 4 5 6\", \"expectedOutput\": \"1 2 3 4 5 6\"}, {\"input\": \"10\\n10 9 8 7 6 5 4 3 2 1\", \"expectedOutput\": \"1 2 3 4 5 6 7 8 9 10\"}]}');
INSERT INTO `question` VALUES (41, 'CODE', '已有 student 表结构如下：\n  CREATE TABLE student (id INT, name TEXT, score INT);\n已插入以下数据：\n  INSERT INTO student VALUES (1, \'张三\', 85), (2, \'李四\', 92), (3, \'王五\', 78), (4, \'赵六\', 88), (5, \'孙七\', 95);\n\n请编写 SQL 查询：查询成绩大于等于 85 分的学生姓名和成绩，按成绩降序排列。\n\n输出格式：\n每行一个学生，格式为 \"姓名|成绩\"。', NULL, '', '使用 WHERE 子句过滤 score >= 85，使用 ORDER BY 降序排列。注意输出格式需要拼接字段。', 10.00, 2, 'SQL-查询语句', 2, NULL, '2026-07-11 20:11:52', '2026-07-11 20:16:54', 0, '{\"language\": \"sql\", \"testCases\": [{\"input\": \"\", \"expectedOutput\": \"孙七|95\\n李四|92\\n赵六|88\\n张三|85\"}], \"timeLimit\": 3, \"memoryLimit\": 128, \"scoringMode\": \"perCase\", \"templateCode\": \"-- 创建表并插入数据\\nCREATE TABLE student (id INT, name TEXT, score INT);\\nINSERT INTO student VALUES (1, \'张三\', 85), (2, \'李四\', 92), (3, \'王五\', 78), (4, \'赵六\', 88), (5, \'孙七\', 95);\\n\\n-- 在此编写查询语句\\nSELECT name || \'|\' || score FROM student WHERE score >= 85 ORDER BY score DESC;\", \"hiddenTestCases\": [{\"input\": \"\", \"expectedOutput\": \"孙七|95\\n李四|92\\n赵六|88\\n张三|85\"}]}');

-- ----------------------------
-- Table structure for question_post
-- ----------------------------
DROP TABLE IF EXISTS `question_post`;
CREATE TABLE `question_post`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint NULL DEFAULT NULL COMMENT '所属课程ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '帖子标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '帖子内容',
  `post_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '提问' COMMENT '类型：提问 / 讨论',
  `create_by` bigint NULL DEFAULT NULL COMMENT '发帖人用户ID',
  `view_count` int NULL DEFAULT 0 COMMENT '浏览次数',
  `reply_count` int NULL DEFAULT 0 COMMENT '回复次数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_course_id`(`course_id` ASC) USING BTREE,
  INDEX `idx_create_by`(`create_by` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '讨论帖/提问表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of question_post
-- ----------------------------
INSERT INTO `question_post` VALUES (1, 1, 'c++与rust哪个是更好的语言？', '', '讨论', 2, 15, 3, '2026-07-11 15:51:35', '2026-07-11 15:51:35', 0);

-- ----------------------------
-- Table structure for schedule
-- ----------------------------
DROP TABLE IF EXISTS `schedule`;
CREATE TABLE `schedule`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '日程标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '日程详细内容',
  `schedule_date` date NOT NULL COMMENT '日程日期',
  `start_time` time NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` time NULL DEFAULT NULL COMMENT '结束时间',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'PERSONAL' COMMENT '类型：PERSONAL(个人) / COURSE(课程) / EXAM(考试) / TRAINING(实训)',
  `course_id` bigint NULL DEFAULT NULL COMMENT '关联课程ID',
  `color` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '#409EFF' COMMENT '日程颜色',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_schedule_date`(`schedule_date` ASC) USING BTREE,
  INDEX `idx_user_date_deleted`(`user_id` ASC, `schedule_date` ASC, `is_deleted` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '日程表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of schedule
-- ----------------------------
INSERT INTO `schedule` VALUES (1, 2, '系统测试', NULL, '2026-07-13', '07:30:50', '11:45:51', 'PERSONAL', NULL, '#E6A23C', '2026-07-13 08:46:22', '2026-07-13 08:50:00', 1);

-- ----------------------------
-- Table structure for student_material
-- ----------------------------
DROP TABLE IF EXISTS `student_material`;
CREATE TABLE `student_material`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` bigint NOT NULL COMMENT '学生用户ID',
  `course_id` bigint NULL DEFAULT NULL COMMENT '关联课程ID',
  `course_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '课程名称(冗余，方便展示)',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '资料标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '资料描述',
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件访问URL',
  `file_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '原始文件名',
  `file_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件类型：PDF/PPT/DOCX/IMAGE/OTHER',
  `file_size` bigint NULL DEFAULT 0 COMMENT '文件大小（字节）',
  `tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签(逗号分隔)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_student_id`(`student_id` ASC) USING BTREE,
  INDEX `idx_course_id`(`course_id` ASC) USING BTREE,
  INDEX `idx_file_type`(`file_type` ASC) USING BTREE,
  INDEX `idx_student_deleted`(`student_id` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学生上传资料表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of student_material
-- ----------------------------
INSERT INTO `student_material` VALUES (1, 3, 1, 'c++', '8209230308-刘麒源-学生实训日志', '', '/uploads/common/0424a5e9-97a1-4234-a6cc-452cbd597ef6_8209230308-刘麒源-学生实训日志.docx', '8209230308-刘麒源-学生实训日志.docx', 'DOCX', 84838, '实训', '2026-07-13 11:42:17', '2026-07-13 11:42:17', 0);

-- ----------------------------
-- Table structure for student_profile
-- ----------------------------
DROP TABLE IF EXISTS `student_profile`;
CREATE TABLE `student_profile`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '关联sys_user.id',
  `student_no` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学号',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `grade` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '年级（如2024级）',
  `major` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '专业',
  `class_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '班级',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像URL',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  UNIQUE INDEX `idx_student_no`(`student_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学生扩展信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of student_profile
-- ----------------------------
INSERT INTO `student_profile` VALUES (1, 3, '20260001', 'pinellia', '2024', '软件工程', '2406', NULL, NULL, NULL, 0);

-- ----------------------------
-- Table structure for student_wrong_question
-- ----------------------------
DROP TABLE IF EXISTS `student_wrong_question`;
CREATE TABLE `student_wrong_question`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` bigint NOT NULL COMMENT '学生用户ID',
  `question_id` bigint NOT NULL COMMENT '试题ID',
  `course_id` bigint NULL DEFAULT NULL COMMENT '关联课程ID',
  `course_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '课程名称(冗余，方便展示)',
  `source_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '来源：EXAM / CLASS_QUIZ',
  `source_id` bigint NULL DEFAULT NULL COMMENT '来源记录ID(exam_id或quiz_id)',
  `question_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '题型：SINGLE/MULTI/JUDGE/FILL/ESSAY',
  `question_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '题目内容',
  `options` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '选项(JSON)',
  `wrong_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '学生错误答案',
  `correct_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '正确答案',
  `analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '题目解析',
  `knowledge_point` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '知识点',
  `is_mastered` tinyint(1) NULL DEFAULT 0 COMMENT '是否已掌握：0否/1是',
  `wrong_count` int NULL DEFAULT 1 COMMENT '累计答错次数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_student_id`(`student_id` ASC) USING BTREE,
  INDEX `idx_course_id`(`course_id` ASC) USING BTREE,
  INDEX `idx_question_student`(`question_id` ASC, `student_id` ASC) USING BTREE,
  INDEX `idx_source_type`(`source_type` ASC) USING BTREE,
  INDEX `idx_mastered`(`is_mastered` ASC) USING BTREE,
  INDEX `idx_student_mastered_deleted`(`student_id` ASC, `is_mastered` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学生错题集表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of student_wrong_question
-- ----------------------------

-- ----------------------------
-- Table structure for survey
-- ----------------------------
DROP TABLE IF EXISTS `survey`;
CREATE TABLE `survey`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '问卷标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '问卷说明',
  `course_id` bigint NULL DEFAULT NULL COMMENT '所属课程ID',
  `start_date` date NULL DEFAULT NULL COMMENT '开始日期',
  `end_date` date NULL DEFAULT NULL COMMENT '结束日期',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '未开始' COMMENT '状态：未开始 / 进行中 / 已结束',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建教师ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_course_id`(`course_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '问卷表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of survey
-- ----------------------------

-- ----------------------------
-- Table structure for survey_question
-- ----------------------------
DROP TABLE IF EXISTS `survey_question`;
CREATE TABLE `survey_question`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `survey_id` bigint NOT NULL COMMENT '问卷ID',
  `question_content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '题目内容',
  `question_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '题型：SINGLE / MULTI / TEXT',
  `options` json NULL COMMENT '选项（JSON格式）',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序号',
  `is_required` tinyint(1) NULL DEFAULT 1 COMMENT '是否必填',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_survey_id`(`survey_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '问卷题目表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of survey_question
-- ----------------------------

-- ----------------------------
-- Table structure for survey_response
-- ----------------------------
DROP TABLE IF EXISTS `survey_response`;
CREATE TABLE `survey_response`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `survey_id` bigint NOT NULL COMMENT '问卷ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `respondent_id` bigint NOT NULL COMMENT '回答人用户ID',
  `answer_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '回答内容',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_survey_id`(`survey_id` ASC) USING BTREE,
  INDEX `idx_respondent_id`(`respondent_id` ASC) USING BTREE,
  INDEX `idx_survey_respondent_deleted`(`survey_id` ASC, `question_id` ASC, `respondent_id` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '问卷回答表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of survey_response
-- ----------------------------

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色编码（ROLE_TEACHER / ROLE_STUDENT / ROLE_ADMIN）',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除：0正常 / 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_role_code`(`role_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, 'ROLE_ADMIN', '管理员', '2026-07-09 20:04:29', '2026-07-09 20:04:29', 0);
INSERT INTO `sys_role` VALUES (2, 'ROLE_TEACHER', '教师', '2026-07-09 20:04:29', '2026-07-09 20:04:29', 0);
INSERT INTO `sys_role` VALUES (3, 'ROLE_STUDENT', '学生', '2026-07-09 20:04:29', '2026-07-09 20:04:29', 0);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（BCrypt加密）',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像URL',
  `role_id` bigint NULL DEFAULT NULL COMMENT '角色ID（关联sys_role）',
  `status` tinyint(1) NULL DEFAULT 0 COMMENT '状态：0正常 / 1禁用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除：0正常 / 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `idx_email`(`email` ASC) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '核心用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2a$10$eevZq/T5k5IUxg1mPcxMyeNpcrfe7/GzEYQb.anZIm1XQQik4sHs6', 'admin@kechang.com', NULL, '系统管理员', NULL, 1, 0, '2026-07-09 20:04:29', '2026-07-10 20:59:55', 0);
INSERT INTO `sys_user` VALUES (2, 'test@qq.com', '$2a$10$.2vFNn7EDVz02xdw7uYtTOyRR8Oi/RsIio0I5hfm1tj3GVpuM99zi', 'test@qq.com', NULL, '周士博', NULL, 2, 0, NULL, NULL, 0);
INSERT INTO `sys_user` VALUES (3, 'xusheng1@qq.com', '$2a$10$eevZq/T5k5IUxg1mPcxMyeNpcrfe7/GzEYQb.anZIm1XQQik4sHs6', 'xusheng1@qq.com', NULL, 'Pinellia', NULL, 3, 0, NULL, NULL, 0);

-- ----------------------------
-- Table structure for teacher_profile
-- ----------------------------
DROP TABLE IF EXISTS `teacher_profile`;
CREATE TABLE `teacher_profile`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '关联sys_user.id',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '职称（教授/副教授/讲师等）',
  `department` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '所属院系',
  `introduction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '个人简介',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像URL',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '教师扩展信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of teacher_profile
-- ----------------------------
INSERT INTO `teacher_profile` VALUES (1, 2, '周士博', '讲师', '计算机学院', NULL, NULL, NULL, NULL, 0);

-- ----------------------------
-- Table structure for training_material
-- ----------------------------
DROP TABLE IF EXISTS `training_material`;
CREATE TABLE `training_material`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` bigint NOT NULL COMMENT '实训项目ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '资料标题',
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件访问URL',
  `file_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '原始文件名',
  `file_size` bigint NULL DEFAULT 0 COMMENT '文件大小（字节）',
  `create_by` bigint NULL DEFAULT NULL COMMENT '上传教师ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '实训资料表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of training_material
-- ----------------------------

-- ----------------------------
-- Table structure for training_project
-- ----------------------------
DROP TABLE IF EXISTS `training_project`;
CREATE TABLE `training_project`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '实训项目名称',
  `course_id` bigint NULL DEFAULT NULL COMMENT '所属课程ID',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '项目描述',
  `start_date` date NULL DEFAULT NULL COMMENT '开始日期',
  `end_date` date NULL DEFAULT NULL COMMENT '结束日期',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '进行中' COMMENT '状态：进行中 / 已结束',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建教师ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_course_id`(`course_id` ASC) USING BTREE,
  INDEX `idx_create_by_deleted`(`create_by` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '实训项目表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of training_project
-- ----------------------------
INSERT INTO `training_project` VALUES (1, 'Qt学生管理系统', NULL, '', '2026-07-01', '2026-07-31', '进行中', 2, NULL, NULL, 0);

-- ----------------------------
-- Table structure for training_report
-- ----------------------------
DROP TABLE IF EXISTS `training_report`;
CREATE TABLE `training_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `student_id` bigint NOT NULL COMMENT '学生用户ID',
  `report_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '报告内容',
  `attachment_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '附件URL',
  `submit_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `score` decimal(5, 2) NULL DEFAULT NULL COMMENT '评分',
  `comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '评语',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '待批改' COMMENT '状态：待批改 / 已批改',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_task_student`(`task_id` ASC, `student_id` ASC) USING BTREE,
  INDEX `idx_student_id`(`student_id` ASC) USING BTREE,
  INDEX `idx_status_deleted`(`status` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '实训报告表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of training_report
-- ----------------------------

-- ----------------------------
-- Table structure for training_task
-- ----------------------------
DROP TABLE IF EXISTS `training_task`;
CREATE TABLE `training_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` bigint NOT NULL COMMENT '实训项目ID',
  `task_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '任务描述',
  `attachment_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '附件URL',
  `deadline` datetime NULL DEFAULT NULL COMMENT '截止时间',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序号',
  `task_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'TASK' COMMENT '任务类型：TASK(普通任务) / DOCUMENT(文档提交)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '实训任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of training_task
-- ----------------------------
INSERT INTO `training_task` VALUES (1, 1, 'Task1安装QT基本开发环境', '', '', '2026-07-11 00:00:00', 1, 'TASK', NULL, NULL, 0);

-- ----------------------------
-- Table structure for video_annotation
-- ----------------------------
DROP TABLE IF EXISTS `video_annotation`;
CREATE TABLE `video_annotation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '批注用户ID',
  `file_key_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件标识SHA-256，用于查询',
  `file_key` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始文件标识/URL',
  `time_seconds` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '视频时间点，单位秒',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '批注内容',
  `author` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '显示作者',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_file`(`user_id` ASC, `file_key_hash` ASC) USING BTREE,
  INDEX `idx_time`(`time_seconds` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '视频资源批注表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of video_annotation
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
