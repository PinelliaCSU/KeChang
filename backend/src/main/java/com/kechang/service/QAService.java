package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.common.BusinessException;
import com.kechang.common.PageResult;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QAService {

    private final QuestionPostMapper postMapper;
    private final AnswerMapper answerMapper;
    private final CommentMapper commentMapper;

    public PageResult<QuestionPost> listPosts(int page, int size, Long courseId, String orderBy) {
        LambdaQueryWrapper<QuestionPost> w = new LambdaQueryWrapper<>();
        if (courseId != null) {
            w.eq(QuestionPost::getCourseId, courseId);
        }
        if ("reply".equals(orderBy)) {
            w.orderByDesc(QuestionPost::getReplyCount); 
        }else {
            w.orderByDesc(QuestionPost::getCreateTime);
        }
        Page<QuestionPost> p = postMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public void createPost(QuestionPost post) {
        postMapper.insert(post);
    }

    public QuestionPost getPostDetail(Long id) {
        QuestionPost p = postMapper.selectById(id);
        if (p != null) {
            p.setViewCount(p.getViewCount() + 1);
            postMapper.updateById(p);
        }
        return p;
    }

    public void createAnswer(Long postId, String content, Long userId) {
        Answer a = new Answer();
        a.setPostId(postId);
        a.setContent(content);
        a.setCreateBy(userId);
        answerMapper.insert(a);
        QuestionPost p = postMapper.selectById(postId);
        if (p != null) {
            p.setReplyCount(p.getReplyCount() + 1);
            postMapper.updateById(p);
        }
    }

    public String aiAnswer(Long postId) {
        QuestionPost p = postMapper.selectById(postId);
        if (p == null) {
            throw BusinessException.notFound("帖子不存在");
        }
        return "【AI 智能回复】关于「" + p.getTitle() + "」的问题，以下是参考建议：\n\n"
                + "1. 请先查阅课程相关章节的讲义和视频资料。\n"
                + "2. 建议在论坛中搜索类似问题的已有讨论。\n"
                + "3. 如需进一步帮助，可以联系课程教师。\n\n（注：此回复由 AI 自动生成，仅供参考。）";
    }

    public void acceptAnswer(Long answerId) {
        Answer a = answerMapper.selectById(answerId);
        if (a == null) {
            throw BusinessException.notFound("回答不存在");
        }
        a.setIsAccepted(1);
        answerMapper.updateById(a);
    }

    public void createComment(String targetType, Long targetId, String content, Long userId) {
        Comment c = new Comment();
        c.setTargetType(targetType);
        c.setTargetId(targetId);
        c.setContent(content);
        c.setCreateBy(userId);
        commentMapper.insert(c);
    }

    public PageResult<Answer> listAnswers(Long postId, int page, int size) {
        Page<Answer> p = answerMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Answer>().eq(Answer::getPostId, postId).orderByDesc(Answer::getIsAccepted));
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }
}
