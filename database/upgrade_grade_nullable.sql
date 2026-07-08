USE kechang;

ALTER TABLE `grade_course`
  MODIFY COLUMN `exam_avg_score` DECIMAL(5,2) DEFAULT NULL COMMENT '考试平均分',
  MODIFY COLUMN `assignment_avg_score` DECIMAL(5,2) DEFAULT NULL COMMENT '作业平均分',
  MODIFY COLUMN `total_score` DECIMAL(5,2) DEFAULT NULL COMMENT '总评成绩';
