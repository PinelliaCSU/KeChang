-- =============================================================
-- 课畅 - 课程级 RAG 分块来源元数据补充
-- 已执行 migration_add_course_knowledge_rag.sql 的数据库可执行本文件。
-- Docker 新库初始化时如果已由建表 SQL 创建字段，本文件也会安全跳过。
-- =============================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS add_course_knowledge_chunk_column $$
CREATE PROCEDURE add_course_knowledge_chunk_column(
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT,
    IN p_after_column VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'course_knowledge_chunk'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'course_knowledge_chunk'
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @sql = CONCAT(
            'ALTER TABLE `course_knowledge_chunk` ADD COLUMN `',
            p_column_name,
            '` ',
            p_column_definition,
            ' AFTER `',
            p_after_column,
            '`'
        );
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

CALL add_course_knowledge_chunk_column('page_no', 'INT DEFAULT NULL COMMENT ''页码：PDF/PPT 等分页资料''', 'source_url') $$
CALL add_course_knowledge_chunk_column('start_time', 'INT DEFAULT NULL COMMENT ''视频/音频分段开始秒数''', 'page_no') $$
CALL add_course_knowledge_chunk_column('end_time', 'INT DEFAULT NULL COMMENT ''视频/音频分段结束秒数''', 'start_time') $$
CALL add_course_knowledge_chunk_column('section_title', 'VARCHAR(255) DEFAULT NULL COMMENT ''章节/标题/题目等语义分段标题''', 'end_time') $$
CALL add_course_knowledge_chunk_column('token_count', 'INT DEFAULT 0 COMMENT ''粗略 token/字符数量''', 'section_title') $$
CALL add_course_knowledge_chunk_column('metadata_json', 'TEXT DEFAULT NULL COMMENT ''来源扩展元数据 JSON''', 'token_count') $$

DROP PROCEDURE IF EXISTS add_course_knowledge_chunk_column $$

DELIMITER ;

SET @table_exists = (
    SELECT COUNT(1)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'course_knowledge_chunk'
);
SET @idx_exists = (
    SELECT COUNT(1)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'course_knowledge_chunk'
      AND INDEX_NAME = 'idx_course_knowledge_chunk_page'
);
SET @sql = IF(@table_exists > 0 AND @idx_exists = 0,
    'CREATE INDEX `idx_course_knowledge_chunk_page` ON `course_knowledge_chunk` (`page_no`)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
