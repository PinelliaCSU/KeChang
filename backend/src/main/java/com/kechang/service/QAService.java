package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.common.BusinessException;
import com.kechang.common.PageResult;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QAService {

    private final QuestionPostMapper postMapper;
    private final AnswerMapper answerMapper;
    private final CommentMapper commentMapper;
    private final SysUserMapper sysUserMapper;
    private final CourseMapper courseMapper;

    // ==================== 用户名 & 课程名回填工具方法 ====================

    /** 回填帖子列表的 createByName 和 courseName */
    private void fillPostNames(List<QuestionPost> posts) {
        if (posts == null || posts.isEmpty()) return;

        // 收集所有 userId
        Set<Long> userIds = posts.stream()
                .map(QuestionPost::getCreateBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        // 收集所有 courseId
        Set<Long> courseIds = posts.stream()
                .map(QuestionPost::getCourseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 批量查询用户
        Map<Long, String> userNames = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
            for (SysUser u : users) {
                userNames.put(u.getId(), u.getNickname() != null && !u.getNickname().isEmpty() ? u.getNickname() : u.getUsername());
            }
        }

        // 批量查询课程
        Map<Long, String> courseNames = new HashMap<>();
        if (!courseIds.isEmpty()) {
            List<Course> courses = courseMapper.selectBatchIds(courseIds);
            for (Course c : courses) {
                courseNames.put(c.getId(), c.getCourseName());
            }
        }

        // 回填
        for (QuestionPost p : posts) {
            p.setCreateByName(userNames.getOrDefault(p.getCreateBy(), "匿名"));
            if (p.getCourseId() != null) {
                p.setCourseName(courseNames.getOrDefault(p.getCourseId(), ""));
            }
        }
    }

    /** 回填单个帖子的 createByName 和 courseName */
    private void fillPostName(QuestionPost post) {
        if (post == null) return;
        fillPostNames(Collections.singletonList(post));
    }

    /** 回填回答列表的 createByName */
    private void fillAnswerNames(List<Answer> answers) {
        if (answers == null || answers.isEmpty()) return;
        Set<Long> userIds = answers.stream()
                .map(Answer::getCreateBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> userNames = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
            for (SysUser u : users) {
                userNames.put(u.getId(), u.getNickname() != null && !u.getNickname().isEmpty() ? u.getNickname() : u.getUsername());
            }
        }

        for (Answer a : answers) {
            a.setCreateByName(userNames.getOrDefault(a.getCreateBy(), "匿名"));
        }
    }

    /** 回填评论列表的 createByName */
    private void fillCommentNames(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) return;
        Set<Long> userIds = comments.stream()
                .map(Comment::getCreateBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> userNames = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
            for (SysUser u : users) {
                userNames.put(u.getId(), u.getNickname() != null && !u.getNickname().isEmpty() ? u.getNickname() : u.getUsername());
            }
        }

        for (Comment c : comments) {
            c.setCreateByName(userNames.getOrDefault(c.getCreateBy(), "匿名"));
        }
    }

    // ==================== 帖子 CRUD ====================

    public PageResult<QuestionPost> listPosts(int page, int size, Long courseId, String orderBy) {
        LambdaQueryWrapper<QuestionPost> w = new LambdaQueryWrapper<>();
        if (courseId != null) {
            w.eq(QuestionPost::getCourseId, courseId);
        }
        // 排序：reply / view / time(默认)
        if ("reply".equals(orderBy)) {
            w.orderByDesc(QuestionPost::getReplyCount);
        } else if ("view".equals(orderBy)) {
            w.orderByDesc(QuestionPost::getViewCount);
        } else {
            w.orderByDesc(QuestionPost::getCreateTime);
        }
        Page<QuestionPost> p = postMapper.selectPage(new Page<>(page, size), w);
        fillPostNames(p.getRecords());
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public void createPost(QuestionPost post) {
        if (post.getCourseId() == null) {
            throw BusinessException.badRequest("请选择所属课程");
        }
        if (post.getPostType() == null || post.getPostType().isEmpty()) {
            post.setPostType("提问");
        }
        postMapper.insert(post);
    }

    public QuestionPost getPostDetail(Long id) {
        QuestionPost p = postMapper.selectById(id);
        if (p != null) {
            p.setViewCount(p.getViewCount() + 1);
            postMapper.updateById(p);
            fillPostName(p);
        }
        return p;
    }

    public void deletePost(Long postId, Long userId) {
        QuestionPost p = postMapper.selectById(postId);
        if (p == null) {
            throw BusinessException.notFound("帖子不存在");
        }
        // 发帖人或教师可删除
        SysUser user = sysUserMapper.selectById(userId);
        boolean isTeacher = user != null && user.getRoleId() != null && user.getRoleId() == 2; // ROLE_TEACHER
        if (!p.getCreateBy().equals(userId) && !isTeacher) {
            throw BusinessException.forbidden("无权删除此帖子");
        }
        postMapper.deleteById(postId);
    }

    // ==================== 回答 CRUD ====================

    public void createAnswer(Long postId, String content, Long userId) {
        QuestionPost p = postMapper.selectById(postId);
        if (p == null) {
            throw BusinessException.notFound("帖子不存在");
        }
        Answer a = new Answer();
        a.setPostId(postId);
        a.setContent(content);
        a.setCreateBy(userId);
        answerMapper.insert(a);
        p.setReplyCount(p.getReplyCount() + 1);
        postMapper.updateById(p);
    }

    public void deleteAnswer(Long answerId, Long userId) {
        Answer a = answerMapper.selectById(answerId);
        if (a == null) {
            throw BusinessException.notFound("回答不存在");
        }
        // 回答人或教师可删除
        SysUser user = sysUserMapper.selectById(userId);
        boolean isTeacher = user != null && user.getRoleId() != null && user.getRoleId() == 2;
        if (!a.getCreateBy().equals(userId) && !isTeacher) {
            throw BusinessException.forbidden("无权删除此回答");
        }
        answerMapper.deleteById(answerId);
        // 帖子回复数减 1
        QuestionPost p = postMapper.selectById(a.getPostId());
        if (p != null && p.getReplyCount() > 0) {
            p.setReplyCount(p.getReplyCount() - 1);
            postMapper.updateById(p);
        }
    }

    public PageResult<Answer> listAnswers(Long postId, int page, int size) {
        Page<Answer> p = answerMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Answer>()
                        .eq(Answer::getPostId, postId)
                        .orderByDesc(Answer::getIsAccepted)
                        .orderByDesc(Answer::getCreateTime));
        fillAnswerNames(p.getRecords());

        // 批量查询每个回答的评论数
        if (!p.getRecords().isEmpty()) {
            List<Long> answerIds = p.getRecords().stream().map(Answer::getId).collect(Collectors.toList());
            List<Comment> comments = commentMapper.selectList(
                    new LambdaQueryWrapper<Comment>()
                            .eq(Comment::getTargetType, "ANSWER")
                            .in(Comment::getTargetId, answerIds));
            Map<Long, Long> countMap = comments.stream()
                    .collect(Collectors.groupingBy(Comment::getTargetId, Collectors.counting()));
            for (Answer a : p.getRecords()) {
                a.setCommentCount(countMap.getOrDefault(a.getId(), 0L).intValue());
            }
        }

        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    // ==================== AI 回答 ====================

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

    // ==================== 采纳回答 ====================

    public void acceptAnswer(Long answerId) {
        Answer a = answerMapper.selectById(answerId);
        if (a == null) {
            throw BusinessException.notFound("回答不存在");
        }
        a.setIsAccepted(1);
        answerMapper.updateById(a);
    }

    // ==================== 评论 CRUD ====================

    public void createComment(String targetType, Long targetId, String content, Long userId) {
        Comment c = new Comment();
        c.setTargetType(targetType);
        c.setTargetId(targetId);
        c.setContent(content);
        c.setCreateBy(userId);
        commentMapper.insert(c);
    }

    public PageResult<Comment> listComments(String targetType, Long targetId, int page, int size) {
        Page<Comment> p = commentMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getTargetType, targetType)
                        .eq(Comment::getTargetId, targetId)
                        .orderByAsc(Comment::getCreateTime));
        fillCommentNames(p.getRecords());
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }
}
