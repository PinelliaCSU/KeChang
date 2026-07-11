-- ============================================
-- 课畅 性能优化迁移 SQL
-- 创建日期: 2026-07-11
-- 说明: 补充关键缺失索引 + 清理冗余索引 + 添加唯一约束
-- ============================================

-- ============================================
-- Part 1: is_deleted 复合索引（高频写入/删除表）
-- MyBatis-Plus 每条 WHERE 都带 is_deleted=0
-- 不加此列会导致回表扫描
-- ============================================

-- exam_record: 考试记录（数据量大，写入频繁）
ALTER TABLE exam_record ADD INDEX idx_exam_submitted_deleted (exam_id, is_submitted, is_deleted);
ALTER TABLE exam_record ADD INDEX idx_student_submitted_deleted (student_id, is_submitted, is_deleted);

-- checkin_record: 签到记录（高频写入）
ALTER TABLE checkin_record ADD INDEX idx_session_deleted (session_id, is_deleted);

-- class_quiz_answer: 随堂答题（高频写入）
ALTER TABLE class_quiz_answer ADD INDEX idx_quiz_student_deleted (quiz_id, student_id, is_deleted);

-- notification_user: 通知记录（批量写入）
ALTER TABLE notification_user ADD INDEX idx_user_read_deleted (user_id, is_read, is_deleted);
ALTER TABLE notification_user ADD INDEX idx_notif_user_deleted (notification_id, user_id, is_deleted);

-- survey_response: 问卷回答
ALTER TABLE survey_response ADD INDEX idx_survey_respondent_deleted (survey_id, question_id, respondent_id, is_deleted);

-- evaluation_result: 评价结果
ALTER TABLE evaluation_result ADD INDEX idx_eval_evaluator_deleted (evaluation_id, evaluator_id, is_deleted);

-- operation_log: 操作日志
ALTER TABLE operation_log ADD INDEX idx_module_action_deleted (module, action, is_deleted);
ALTER TABLE operation_log ADD INDEX idx_target_deleted (target_type, target_id, is_deleted);

-- assignment: 作业
ALTER TABLE assignment ADD INDEX idx_course_status_deleted (course_id, status, is_deleted);
ALTER TABLE assignment ADD INDEX idx_create_by_deleted (create_by, is_deleted);

-- exam: 考试
ALTER TABLE exam ADD INDEX idx_course_status_deleted (course_id, status, is_deleted);

-- assignment_submit: 作业提交
ALTER TABLE assignment_submit ADD INDEX idx_assignment_status_deleted (assignment_id, status, is_deleted);

-- training_project: 实训项目
ALTER TABLE training_project ADD INDEX idx_create_by_deleted (create_by, is_deleted);

-- training_report: 实训报告
ALTER TABLE training_report ADD INDEX idx_status_deleted (status, is_deleted);

-- participation_point: 积分
ALTER TABLE participation_point ADD INDEX idx_teacher_deleted (teacher_id, is_deleted);
ALTER TABLE participation_point ADD INDEX idx_course_student_deleted (course_id, student_id, is_deleted);

-- learning_note: 笔记
ALTER TABLE learning_note ADD INDEX idx_chapter_deleted (chapter_id, is_deleted);

-- announcement: 公告
ALTER TABLE announcement ADD INDEX idx_course_deleted (course_id, is_deleted);

-- ============================================
-- Part 2: 唯一约束（防重复提交）
-- ============================================

-- evaluation_result: 防止同一评价人对同一指标重复提交
-- 先清理可能的重复数据
DELETE e1 FROM evaluation_result e1
INNER JOIN evaluation_result e2
WHERE e1.id > e2.id
  AND e1.evaluation_id = e2.evaluation_id
  AND e1.evaluator_id = e2.evaluator_id
  AND e1.target_user_id = e2.target_user_id
  AND e1.item_id = e2.item_id;
ALTER TABLE evaluation_result ADD UNIQUE INDEX uk_eval_evaluator_target_item (evaluation_id, evaluator_id, target_user_id, item_id);

-- survey_response: 防止同一用户对同一题目重复回答
DELETE s1 FROM survey_response s1
INNER JOIN survey_response s2
WHERE s1.id > s2.id
  AND s1.survey_id = s2.survey_id
  AND s1.question_id = s2.question_id
  AND s1.respondent_id = s2.respondent_id;
ALTER TABLE survey_response ADD UNIQUE INDEX uk_survey_question_respondent (survey_id, question_id, respondent_id);

-- notification_user: 防止重复插入通知
DELETE n1 FROM notification_user n1
INNER JOIN notification_user n2
WHERE n1.id > n2.id
  AND n1.notification_id = n2.notification_id
  AND n1.user_id = n2.user_id;
ALTER TABLE notification_user ADD UNIQUE INDEX uk_notification_user (notification_id, user_id);

-- ============================================
-- Part 3: 清理冗余索引（减少写入开销）
-- ============================================

-- checkin_record.idx_session_id 被 uk_session_student(session_id, student_id) 覆盖
-- 使用 prepared statement 防止索引不存在时报错
SET @sql_drop1 = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE checkin_record DROP INDEX idx_session_id', 'SELECT 1') FROM information_schema.statistics WHERE table_name='checkin_record' AND index_name='idx_session_id');
PREPARE stmt1 FROM @sql_drop1; EXECUTE stmt1; DEALLOCATE PREPARE stmt1;

-- class_quiz_answer.idx_quiz_id 被 idx_quiz_student_question(quiz_id, student_id, question_id) 覆盖
SET @sql_drop2 = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE class_quiz_answer DROP INDEX idx_quiz_id', 'SELECT 1') FROM information_schema.statistics WHERE table_name='class_quiz_answer' AND index_name='idx_quiz_id');
PREPARE stmt2 FROM @sql_drop2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

-- class_quiz_question.idx_quiz_id 被 idx_quiz_question(quiz_id, question_id) 覆盖
SET @sql_drop3 = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE class_quiz_question DROP INDEX idx_quiz_id', 'SELECT 1') FROM information_schema.statistics WHERE table_name='class_quiz_question' AND index_name='idx_quiz_id');
PREPARE stmt3 FROM @sql_drop3; EXECUTE stmt3; DEALLOCATE PREPARE stmt3;

-- ============================================
-- Part 4: 全文索引（问答搜索优化）
-- ============================================

-- question_post.content: 问答帖子搜索
ALTER TABLE question_post ADD FULLTEXT INDEX ft_post_content (content);

-- answer.content: 回答内容搜索
ALTER TABLE answer ADD FULLTEXT INDEX ft_answer_content (content);
