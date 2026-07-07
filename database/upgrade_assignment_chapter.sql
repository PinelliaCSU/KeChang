USE kechang;

ALTER TABLE `assignment`
  ADD COLUMN `chapter_id` BIGINT DEFAULT NULL COMMENT '所属章节ID' AFTER `course_id`,
  ADD INDEX `idx_chapter_id` (`chapter_id`);
