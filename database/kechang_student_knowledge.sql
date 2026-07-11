-- ============================================================
-- 学生端知识库模块 - 建表语句
-- 包含：学生上传资料表、学生错题集表
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for student_material (学生上传资料)
-- ----------------------------
DROP TABLE IF EXISTS `student_material`;
CREATE TABLE `student_material` (
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
  INDEX `idx_student_id` (`student_id`) USING BTREE,
  INDEX `idx_course_id` (`course_id`) USING BTREE,
  INDEX `idx_file_type` (`file_type`) USING BTREE,
  INDEX `idx_student_deleted` (`student_id`, `is_deleted`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学生上传资料表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for student_wrong_question (学生错题集)
-- ----------------------------
DROP TABLE IF EXISTS `student_wrong_question`;
CREATE TABLE `student_wrong_question` (
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
  INDEX `idx_student_id` (`student_id`) USING BTREE,
  INDEX `idx_course_id` (`course_id`) USING BTREE,
  INDEX `idx_question_student` (`question_id`, `student_id`) USING BTREE,
  INDEX `idx_source_type` (`source_type`) USING BTREE,
  INDEX `idx_mastered` (`is_mastered`) USING BTREE,
  INDEX `idx_student_mastered_deleted` (`student_id`, `is_mastered`, `is_deleted`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学生错题集表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
