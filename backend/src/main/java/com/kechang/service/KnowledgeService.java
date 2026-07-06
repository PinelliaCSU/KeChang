package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.common.PageResult;
import com.kechang.entity.KnowledgeArticle;
import com.kechang.entity.LearningNote;
import com.kechang.mapper.KnowledgeArticleMapper;
import com.kechang.mapper.LearningNoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 知识库与笔记服务
 */
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeArticleMapper articleMapper;
    private final LearningNoteMapper noteMapper;

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

    public PageResult<LearningNote> listNotes(Long studentId, int page, int size) {
        Page<LearningNote> p = noteMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<LearningNote>().eq(LearningNote::getStudentId, studentId)
                        .orderByDesc(LearningNote::getCreateTime));
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public void createNote(LearningNote n) {
        noteMapper.insert(n);
    }

    public void updateNote(Long id, LearningNote n) {
        n.setId(id);
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
}
