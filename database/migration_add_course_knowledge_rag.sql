-- =============================================================
-- 课畅 - 课程级智能知识库 RAG
-- MySQL 存文档/分块元信息；正式向量库使用 Redis Stack RediSearch Vector Search，本地开发可使用 local-json 兜底
-- =============================================================

CREATE TABLE IF NOT EXISTS `course_knowledge_document` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `course_id` BIGINT NOT NULL COMMENT '课程ID',
    `title` VARCHAR(255) NOT NULL COMMENT '文档标题',
    `source_type` VARCHAR(40) NOT NULL COMMENT '来源类型：RESOURCE/ASSIGNMENT/QUESTION/QA/MANUAL',
    `source_id` BIGINT DEFAULT NULL COMMENT '来源业务ID',
    `source_url` VARCHAR(500) DEFAULT NULL COMMENT '来源文件或页面地址',
    `status` VARCHAR(20) DEFAULT 'READY' COMMENT '处理状态：PROCESSING/READY/FAILED',
    `chunk_count` INT DEFAULT 0 COMMENT '分块数量',
    `char_count` INT DEFAULT 0 COMMENT '文本字符数',
    `error_message` VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0,
    INDEX `idx_course` (`course_id`),
    INDEX `idx_source` (`source_type`, `source_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程知识库文档';

CREATE TABLE IF NOT EXISTS `course_knowledge_chunk` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `document_id` BIGINT NOT NULL COMMENT '文档ID',
    `course_id` BIGINT NOT NULL COMMENT '课程ID',
    `chunk_index` INT NOT NULL COMMENT '文档内分块序号',
    `content` TEXT NOT NULL COMMENT '分块文本',
    `source_title` VARCHAR(255) DEFAULT NULL COMMENT '来源标题',
    `source_type` VARCHAR(40) NOT NULL COMMENT '来源类型',
    `source_id` BIGINT DEFAULT NULL COMMENT '来源业务ID',
    `source_url` VARCHAR(500) DEFAULT NULL COMMENT '来源文件或页面地址',
    `page_no` INT DEFAULT NULL COMMENT '页码：PDF/PPT 等分页资料',
    `start_time` INT DEFAULT NULL COMMENT '视频/音频分段开始秒数',
    `end_time` INT DEFAULT NULL COMMENT '视频/音频分段结束秒数',
    `section_title` VARCHAR(255) DEFAULT NULL COMMENT '章节/标题/题目等语义分段标题',
    `token_count` INT DEFAULT 0 COMMENT '粗略 token/字符数量',
    `metadata_json` TEXT DEFAULT NULL COMMENT '来源扩展元数据 JSON',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0,
    INDEX `idx_document` (`document_id`),
    INDEX `idx_course` (`course_id`),
    INDEX `idx_source` (`source_type`, `source_id`),
    INDEX `idx_page` (`page_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程知识库分块';
