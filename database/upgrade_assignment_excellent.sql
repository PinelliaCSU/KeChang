ALTER TABLE `assignment_submit`
  ADD COLUMN `is_excellent` TINYINT(1) DEFAULT 0 COMMENT '是否优秀作业' AFTER `status`;
