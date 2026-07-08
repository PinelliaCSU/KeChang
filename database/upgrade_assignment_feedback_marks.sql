USE kechang;

ALTER TABLE `assignment_grade`
  ADD COLUMN `feedback_marks` TEXT DEFAULT NULL COMMENT '结构化批注JSON' AFTER `comment`;
