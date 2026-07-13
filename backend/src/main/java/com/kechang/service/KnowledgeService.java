package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.common.PageResult;
import com.kechang.entity.KnowledgeArticle;
import com.kechang.entity.LearningNote;
import com.kechang.entity.Question;
import com.kechang.entity.StudentMaterial;
import com.kechang.entity.StudentWrongQuestion;
import com.kechang.mapper.KnowledgeArticleMapper;
import com.kechang.mapper.LearningNoteMapper;
import com.kechang.mapper.StudentMaterialMapper;
import com.kechang.mapper.StudentWrongQuestionMapper;
import com.kechang.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 知识库与笔记服务
 */
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeArticleMapper articleMapper;
    private final LearningNoteMapper noteMapper;
    private final StudentMaterialMapper materialMapper;
    private final StudentWrongQuestionMapper wrongQuestionMapper;

    // ==================== 知识库文章 ====================
    public PageResult<KnowledgeArticle> listArticles(int page, int size, String keyword) {
        LambdaQueryWrapper<KnowledgeArticle> w = new LambdaQueryWrapper<>();
        if (keyword != null) {
            w.and(wrapper -> wrapper.like(KnowledgeArticle::getTitle, keyword)
                    .or().like(KnowledgeArticle::getContent, keyword));
        }
        w.eq(KnowledgeArticle::getStatus, "发布").orderByDesc(KnowledgeArticle::getCreateTime);
        Page<KnowledgeArticle> p = articleMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public KnowledgeArticle getArticle(Long id) {
        KnowledgeArticle a = articleMapper.selectById(id);
        if (a != null) {
            a.setViewCount(a.getViewCount() + 1);
            articleMapper.updateById(a);
        }
        return a;
    }

    public void createArticle(KnowledgeArticle a) {
        articleMapper.insert(a);
    }

    // ==================== 学习笔记（已有） ====================
    public PageResult<LearningNote> listNotes(Long studentId, int page, int size) {
        Page<LearningNote> p = noteMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<LearningNote>().eq(LearningNote::getStudentId, studentId)
                        .orderByDesc(LearningNote::getCreateTime));
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public void createNote(LearningNote n) {
        n.setContent(HtmlSanitizer.sanitize(n.getContent()));
        noteMapper.insert(n);
    }

    public void updateNote(Long id, LearningNote n) {
        n.setId(id);
        n.setContent(HtmlSanitizer.sanitize(n.getContent()));
        noteMapper.updateById(n);
    }

    public void deleteNote(Long id) {
        noteMapper.deleteById(id);
    }

    public void togglePublic(Long id) {
        LearningNote n = noteMapper.selectById(id);
        if (n != null) {
            n.setIsPublic(n.getIsPublic() == 1 ? 0 : 1);
            noteMapper.updateById(n);
        }
    }

    // ==================== 我的资料 ====================
    public PageResult<StudentMaterial> listMaterials(Long studentId, int page, int size, Long courseId, String keyword) {
        LambdaQueryWrapper<StudentMaterial> w = new LambdaQueryWrapper<>();
        w.eq(StudentMaterial::getStudentId, studentId);
        if (courseId != null) {
            w.eq(StudentMaterial::getCourseId, courseId);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.and(wrapper -> wrapper.like(StudentMaterial::getTitle, keyword)
                    .or().like(StudentMaterial::getDescription, keyword)
                    .or().like(StudentMaterial::getTags, keyword));
        }
        w.orderByDesc(StudentMaterial::getCreateTime);
        Page<StudentMaterial> p = materialMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public void createMaterial(StudentMaterial material) {
        materialMapper.insert(material);
    }

    public void updateMaterial(Long id, StudentMaterial material, Long studentId) {
        StudentMaterial exist = materialMapper.selectById(id);
        if (exist == null || !exist.getStudentId().equals(studentId)) {
            return;
        }
        material.setId(id);
        materialMapper.updateById(material);
    }

    public void deleteMaterial(Long id, Long studentId) {
        StudentMaterial exist = materialMapper.selectById(id);
        if (exist != null && exist.getStudentId().equals(studentId)) {
            materialMapper.deleteById(id);
        }
    }

    // ==================== 我的错题 ====================
    public PageResult<StudentWrongQuestion> listWrongQuestions(Long studentId, int page, int size,
            Long courseId, Integer isMastered, String keyword) {
        LambdaQueryWrapper<StudentWrongQuestion> w = new LambdaQueryWrapper<>();
        w.eq(StudentWrongQuestion::getStudentId, studentId);
        if (courseId != null) {
            w.eq(StudentWrongQuestion::getCourseId, courseId);
        }
        if (isMastered != null) {
            w.eq(StudentWrongQuestion::getIsMastered, isMastered);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.and(wrapper -> wrapper.like(StudentWrongQuestion::getQuestionContent, keyword)
                    .or().like(StudentWrongQuestion::getKnowledgePoint, keyword));
        }
        w.orderByDesc(StudentWrongQuestion::getCreateTime);
        Page<StudentWrongQuestion> p = wrongQuestionMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    /**
     * 收集错题 - 去重：同一学生+同一试题再次答错时 wrong_count+1
     */
    public void collectWrongQuestion(Long studentId, Question question, Long courseId,
            String courseName, String sourceType, Long sourceId, String wrongAnswer) {
        // 查找是否已存在
        StudentWrongQuestion exist = wrongQuestionMapper.selectOne(new LambdaQueryWrapper<StudentWrongQuestion>()
                .eq(StudentWrongQuestion::getStudentId, studentId)
                .eq(StudentWrongQuestion::getQuestionId, question.getId()));

        if (exist != null) {
            // 已存在：累加错误次数，重置掌握状态
            exist.setWrongCount(exist.getWrongCount() + 1);
            exist.setIsMastered(0);
            exist.setWrongAnswer(wrongAnswer);
            wrongQuestionMapper.updateById(exist);
        } else {
            // 新错题：插入
            StudentWrongQuestion wq = new StudentWrongQuestion();
            wq.setStudentId(studentId);
            wq.setQuestionId(question.getId());
            wq.setCourseId(courseId);
            wq.setCourseName(courseName);
            wq.setSourceType(sourceType);
            wq.setSourceId(sourceId);
            wq.setQuestionType(question.getQuestionType());
            wq.setQuestionContent(question.getContent());
            wq.setOptions(question.getOptions());
            wq.setWrongAnswer(wrongAnswer);
            wq.setCorrectAnswer(question.getAnswer());
            wq.setAnalysis(question.getAnalysis());
            wq.setKnowledgePoint(question.getKnowledgePoint());
            wq.setWrongCount(1);
            wq.setIsMastered(0);
            wrongQuestionMapper.insert(wq);
        }
    }

    public void markMastered(Long id, Long studentId) {
        StudentWrongQuestion wq = wrongQuestionMapper.selectById(id);
        if (wq != null && wq.getStudentId().equals(studentId)) {
            wq.setIsMastered(wq.getIsMastered() == 1 ? 0 : 1);
            wrongQuestionMapper.updateById(wq);
        }
    }

    public void deleteWrongQuestion(Long id, Long studentId) {
        StudentWrongQuestion wq = wrongQuestionMapper.selectById(id);
        if (wq != null && wq.getStudentId().equals(studentId)) {
            wrongQuestionMapper.deleteById(id);
        }
    }

    // ==================== 统计 ====================
    public Map<String, Object> getKnowledgeStats(Long studentId) {
        Map<String, Object> stats = new HashMap<>();

        long noteCount = noteMapper.selectCount(new LambdaQueryWrapper<LearningNote>()
                .eq(LearningNote::getStudentId, studentId));
        long materialCount = materialMapper.selectCount(new LambdaQueryWrapper<StudentMaterial>()
                .eq(StudentMaterial::getStudentId, studentId));
        long wrongQuestionCount = wrongQuestionMapper.selectCount(new LambdaQueryWrapper<StudentWrongQuestion>()
                .eq(StudentWrongQuestion::getStudentId, studentId));
        long unmasteredCount = wrongQuestionMapper.selectCount(new LambdaQueryWrapper<StudentWrongQuestion>()
                .eq(StudentWrongQuestion::getStudentId, studentId)
                .eq(StudentWrongQuestion::getIsMastered, 0));

        stats.put("noteCount", noteCount);
        stats.put("materialCount", materialCount);
        stats.put("wrongQuestionCount", wrongQuestionCount);
        stats.put("unmasteredCount", unmasteredCount);
        return stats;
    }
}
