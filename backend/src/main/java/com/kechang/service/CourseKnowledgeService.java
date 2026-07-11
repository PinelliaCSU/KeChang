package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kechang.common.BusinessException;
import com.kechang.dto.CourseKnowledgeAskDTO;
import com.kechang.dto.CourseKnowledgeManualDTO;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import com.kechang.service.vector.CourseVectorStore;
import com.kechang.service.vector.VectorRecord;
import com.kechang.service.vector.VectorSearchResult;
import com.kechang.vo.CourseKnowledgeAskVO;
import com.kechang.vo.CourseKnowledgeStatsVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseKnowledgeService {

    private final CourseMapper courseMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final CourseResourceMapper resourceMapper;
    private final AssignmentMapper assignmentMapper;
    private final QuestionMapper questionMapper;
    private final QuestionPostMapper postMapper;
    private final AnswerMapper answerMapper;
    private final CourseKnowledgeDocumentMapper documentMapper;
    private final CourseKnowledgeChunkMapper chunkMapper;
    private final FileProcessingService fileProcessingService;
    private final BailianApiService bailianApiService;
    private final CourseVectorStore vectorStore;

    public List<CourseKnowledgeStatsVO> listAvailableCourses(Long userId, String role) {
        List<Course> courses;
        if ("ROLE_ADMIN".equals(role)) {
            courses = courseMapper.selectList(new LambdaQueryWrapper<Course>().orderByDesc(Course::getCreateTime));
        } else if ("ROLE_TEACHER".equals(role)) {
            courses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                    .eq(Course::getTeacherId, userId)
                    .orderByDesc(Course::getCreateTime));
        } else {
            List<Long> ids = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                            .eq(CourseStudent::getStudentId, userId))
                    .stream().map(CourseStudent::getCourseId).toList();
            if (ids.isEmpty()) {
                return List.of();
            }
            courses = courseMapper.selectBatchIds(ids);
        }
        return courses.stream().map(this::statsForCourse).toList();
    }

    public List<CourseKnowledgeDocument> listDocuments(Long courseId, Long userId, String role) {
        checkReadable(courseId, userId, role);
        if (!isKnowledgeStorageReady()) {
            return List.of();
        }
        return documentMapper.selectList(new LambdaQueryWrapper<CourseKnowledgeDocument>()
                .eq(CourseKnowledgeDocument::getCourseId, courseId)
                .orderByDesc(CourseKnowledgeDocument::getUpdateTime));
    }

    public CourseKnowledgeStatsVO getStats(Long courseId, Long userId, String role) {
        checkReadable(courseId, userId, role);
        Course course = requireCourse(courseId);
        return statsForCourse(course);
    }

    @Transactional
    public Map<String, Object> rebuildCourse(Long courseId, Long teacherId) {
        checkManageable(courseId, teacherId, "ROLE_TEACHER");
        ensureKnowledgeStorageReady();
        deleteCourseKnowledge(courseId);

        List<KnowledgeSource> sources = new ArrayList<>();
        sources.addAll(resourceSources(courseId));
        sources.addAll(assignmentSources(courseId));
        sources.addAll(questionSources(courseId));
        sources.addAll(postSources(courseId));

        int chunks = 0;
        int docs = 0;
        for (KnowledgeSource source : sources) {
            CourseKnowledgeDocument doc = indexSource(courseId, source, teacherId);
            docs++;
            chunks += doc.getChunkCount() == null ? 0 : doc.getChunkCount();
        }
        return Map.of("documents", docs, "chunks", chunks);
    }

    @Transactional
    public CourseKnowledgeDocument addManual(CourseKnowledgeManualDTO dto, Long teacherId, String role) {
        checkManageable(dto.getCourseId(), teacherId, role);
        ensureKnowledgeStorageReady();
        KnowledgeSource source = new KnowledgeSource();
        source.setTitle(dto.getTitle());
        source.setContent(dto.getContent());
        source.setSourceType("MANUAL");
        return indexSource(dto.getCourseId(), source, teacherId);
    }

    @Transactional
    public void deleteDocument(Long id, Long userId, String role) {
        ensureKnowledgeStorageReady();
        CourseKnowledgeDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw BusinessException.notFound("知识文档不存在");
        }
        checkManageable(document.getCourseId(), userId, role);
        chunkMapper.delete(new LambdaQueryWrapper<CourseKnowledgeChunk>()
                .eq(CourseKnowledgeChunk::getDocumentId, id));
        vectorStore.deleteByDocument(id);
        documentMapper.deleteById(id);
    }

    public CourseKnowledgeAskVO ask(CourseKnowledgeAskDTO dto, Long userId, String role) {
        checkReadable(dto.getCourseId(), userId, role);
        ensureKnowledgeStorageReady();
        Course course = requireCourse(dto.getCourseId());
        List<Double> queryVector = bailianApiService.embedText(dto.getQuestion());
        int topK = dto.getTopK() == null ? 5 : Math.max(1, Math.min(10, dto.getTopK()));
        List<VectorSearchResult> hits = vectorStore.search(dto.getCourseId(), queryVector, topK);
        if (hits.isEmpty()) {
            CourseKnowledgeAskVO vo = new CourseKnowledgeAskVO();
            vo.setAnswer("这个课程知识库还没有可检索的内容。请先让教师重建知识库或添加资料。");
            vo.setReferences(List.of());
            return vo;
        }

        Map<Long, Double> scoreMap = hits.stream()
                .collect(Collectors.toMap(VectorSearchResult::getChunkId, VectorSearchResult::getScore, (a, b) -> a, LinkedHashMap::new));
        List<CourseKnowledgeChunk> chunks = chunkMapper.selectBatchIds(scoreMap.keySet()).stream()
                .sorted(Comparator.comparingInt(item -> new ArrayList<>(scoreMap.keySet()).indexOf(item.getId())))
                .toList();

        StringBuilder context = new StringBuilder();
        List<CourseKnowledgeAskVO.Reference> references = new ArrayList<>();
        int index = 1;
        for (CourseKnowledgeChunk chunk : chunks) {
            context.append("[").append(index).append("] ")
                    .append(chunk.getSourceTitle()).append(" / ").append(chunk.getSourceType()).append("\n")
                    .append(chunk.getContent()).append("\n\n");
            CourseKnowledgeAskVO.Reference ref = new CourseKnowledgeAskVO.Reference();
            ref.setChunkId(chunk.getId());
            ref.setDocumentId(chunk.getDocumentId());
            ref.setTitle(chunk.getSourceTitle());
            ref.setSourceType(chunk.getSourceType());
            ref.setSourceUrl(chunk.getSourceUrl());
            ref.setScore(scoreMap.getOrDefault(chunk.getId(), 0.0));
            ref.setSnippet(snippet(chunk.getContent(), 180));
            references.add(ref);
            index++;
        }

        CourseKnowledgeAskVO vo = new CourseKnowledgeAskVO();
        vo.setAnswer(bailianApiService.answerWithContext(dto.getQuestion(), context.toString(), course.getCourseName()));
        vo.setReferences(references);
        return vo;
    }

    private CourseKnowledgeDocument indexSource(Long courseId, KnowledgeSource source, Long createBy) {
        CourseKnowledgeDocument doc = new CourseKnowledgeDocument();
        doc.setCourseId(courseId);
        doc.setTitle(source.getTitle());
        doc.setSourceType(source.getSourceType());
        doc.setSourceId(source.getSourceId());
        doc.setSourceUrl(source.getSourceUrl());
        doc.setCreateBy(createBy);
        doc.setStatus("PROCESSING");
        doc.setChunkCount(0);
        doc.setCharCount(source.getContent() == null ? 0 : source.getContent().length());
        documentMapper.insert(doc);

        try {
            List<String> chunks = splitChunks(source.getContent());
            int index = 0;
            for (String text : chunks) {
                CourseKnowledgeChunk chunk = new CourseKnowledgeChunk();
                chunk.setDocumentId(doc.getId());
                chunk.setCourseId(courseId);
                chunk.setChunkIndex(index);
                chunk.setContent(text);
                chunk.setSourceTitle(source.getTitle());
                chunk.setSourceType(source.getSourceType());
                chunk.setSourceId(source.getSourceId());
                chunk.setSourceUrl(source.getSourceUrl());
                chunkMapper.insert(chunk);

                VectorRecord record = new VectorRecord();
                record.setCourseId(courseId);
                record.setDocumentId(doc.getId());
                record.setChunkId(chunk.getId());
                record.setVector(bailianApiService.embedText(text));
                vectorStore.upsert(record);
                index++;
            }
            doc.setChunkCount(chunks.size());
            doc.setStatus("READY");
            documentMapper.updateById(doc);
        } catch (Exception e) {
            doc.setStatus("FAILED");
            doc.setErrorMessage(e.getMessage());
            documentMapper.updateById(doc);
        }
        return doc;
    }

    private List<KnowledgeSource> resourceSources(Long courseId) {
        return resourceMapper.selectList(new LambdaQueryWrapper<CourseResource>().eq(CourseResource::getCourseId, courseId))
                .stream().map(resource -> {
                    String content = fileProcessingService.extractText(resource.getFileUrl(), resource.getResourceName());
                    if (!StringUtils.hasText(content)) {
                        content = "资源名称：" + resource.getResourceName() + "\n类型：" + resource.getResourceType();
                    }
                    KnowledgeSource source = new KnowledgeSource();
                    source.setTitle(resource.getResourceName());
                    source.setContent(content);
                    source.setSourceType("RESOURCE");
                    source.setSourceId(resource.getId());
                    source.setSourceUrl(resource.getFileUrl());
                    return source;
                }).filter(source -> StringUtils.hasText(source.getContent())).toList();
    }

    private List<KnowledgeSource> assignmentSources(Long courseId) {
        return assignmentMapper.selectList(new LambdaQueryWrapper<Assignment>().eq(Assignment::getCourseId, courseId))
                .stream().map(item -> {
                    StringBuilder content = new StringBuilder();
                    content.append("作业标题：").append(item.getTitle()).append("\n");
                    content.append("作业说明：").append(item.getContent() == null ? "" : item.getContent()).append("\n");
                    if (StringUtils.hasText(item.getAttachmentUrl())) {
                        content.append("附件内容：\n")
                                .append(fileProcessingService.extractText(item.getAttachmentUrl(), item.getTitle()));
                    }
                    KnowledgeSource source = new KnowledgeSource();
                    source.setTitle(item.getTitle());
                    source.setContent(content.toString());
                    source.setSourceType("ASSIGNMENT");
                    source.setSourceId(item.getId());
                    return source;
                }).filter(source -> StringUtils.hasText(source.getContent())).toList();
    }

    private List<KnowledgeSource> questionSources(Long courseId) {
        return questionMapper.selectList(new LambdaQueryWrapper<Question>().eq(Question::getCourseId, courseId))
                .stream().map(item -> {
                    String content = "题目：" + safe(item.getContent())
                            + "\n选项：" + safe(item.getOptions())
                            + "\n答案：" + safe(item.getAnswer())
                            + "\n解析：" + safe(item.getAnalysis())
                            + "\n知识点：" + safe(item.getKnowledgePoint());
                    KnowledgeSource source = new KnowledgeSource();
                    source.setTitle("题库解析 #" + item.getId());
                    source.setContent(content);
                    source.setSourceType("QUESTION");
                    source.setSourceId(item.getId());
                    return source;
                }).filter(source -> StringUtils.hasText(source.getContent())).toList();
    }

    private List<KnowledgeSource> postSources(Long courseId) {
        List<QuestionPost> posts = postMapper.selectList(new LambdaQueryWrapper<QuestionPost>().eq(QuestionPost::getCourseId, courseId));
        List<KnowledgeSource> sources = new ArrayList<>();
        for (QuestionPost post : posts) {
            StringBuilder content = new StringBuilder();
            content.append("问答标题：").append(post.getTitle()).append("\n");
            content.append("问题内容：").append(safe(post.getContent())).append("\n");
            List<Answer> answers = answerMapper.selectList(new LambdaQueryWrapper<Answer>()
                    .eq(Answer::getPostId, post.getId())
                    .orderByDesc(Answer::getIsAccepted)
                    .orderByAsc(Answer::getCreateTime));
            for (Answer answer : answers) {
                content.append("回复：").append(safe(answer.getContent())).append("\n");
            }
            KnowledgeSource source = new KnowledgeSource();
            source.setTitle(post.getTitle());
            source.setContent(content.toString());
            source.setSourceType("QA");
            source.setSourceId(post.getId());
            sources.add(source);
        }
        return sources;
    }

    private List<String> splitChunks(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        String clean = content.replace("\r", "\n").replaceAll("\\n{3,}", "\n\n").trim();
        int size = 900;
        int overlap = 120;
        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < clean.length(); start += size - overlap) {
            int end = Math.min(clean.length(), start + size);
            String part = clean.substring(start, end).trim();
            if (part.length() >= 20) {
                chunks.add(part);
            }
            if (end >= clean.length()) {
                break;
            }
        }
        return chunks;
    }

    private void deleteCourseKnowledge(Long courseId) {
        documentMapper.delete(new LambdaQueryWrapper<CourseKnowledgeDocument>().eq(CourseKnowledgeDocument::getCourseId, courseId));
        chunkMapper.delete(new LambdaQueryWrapper<CourseKnowledgeChunk>().eq(CourseKnowledgeChunk::getCourseId, courseId));
        vectorStore.deleteByCourse(courseId);
    }

    private CourseKnowledgeStatsVO statsForCourse(Course course) {
        CourseKnowledgeStatsVO vo = new CourseKnowledgeStatsVO();
        vo.setCourseId(course.getId());
        vo.setCourseName(course.getCourseName());
        vo.setSourceResourceCount(resourceMapper.selectCount(new LambdaQueryWrapper<CourseResource>()
                .eq(CourseResource::getCourseId, course.getId())));
        vo.setSourceAssignmentCount(assignmentMapper.selectCount(new LambdaQueryWrapper<Assignment>()
                .eq(Assignment::getCourseId, course.getId())));
        vo.setSourceQuestionCount(questionMapper.selectCount(new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, course.getId())));
        vo.setSourceQaCount(postMapper.selectCount(new LambdaQueryWrapper<QuestionPost>()
                .eq(QuestionPost::getCourseId, course.getId())));
        try {
            List<CourseKnowledgeDocument> docs = documentMapper.selectList(new LambdaQueryWrapper<CourseKnowledgeDocument>()
                    .eq(CourseKnowledgeDocument::getCourseId, course.getId()));
            vo.setDocumentCount((long) docs.size());
            vo.setChunkCount(chunkMapper.selectCount(new LambdaQueryWrapper<CourseKnowledgeChunk>()
                    .eq(CourseKnowledgeChunk::getCourseId, course.getId())));
            vo.setReadyDocumentCount((int) docs.stream().filter(item -> "READY".equals(item.getStatus())).count());
            vo.setFailedDocumentCount((int) docs.stream().filter(item -> "FAILED".equals(item.getStatus())).count());
            vo.setStorageReady(true);
        } catch (DataAccessException e) {
            vo.setDocumentCount(0L);
            vo.setChunkCount(0L);
            vo.setReadyDocumentCount(0);
            vo.setFailedDocumentCount(0);
            vo.setStorageReady(false);
        }
        return vo;
    }

    private boolean isKnowledgeStorageReady() {
        try {
            documentMapper.selectCount(new LambdaQueryWrapper<CourseKnowledgeDocument>());
            chunkMapper.selectCount(new LambdaQueryWrapper<CourseKnowledgeChunk>());
            return true;
        } catch (DataAccessException e) {
            return false;
        }
    }

    private void ensureKnowledgeStorageReady() {
        if (!isKnowledgeStorageReady()) {
            throw BusinessException.badRequest("课程知识库表未初始化，请先执行 database/migration_add_course_knowledge_rag.sql");
        }
    }

    private Course requireCourse(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BusinessException.notFound("课程不存在");
        }
        return course;
    }

    private void checkReadable(Long courseId, Long userId, String role) {
        Course course = requireCourse(courseId);
        if ("ROLE_ADMIN".equals(role)) {
            return;
        }
        if ("ROLE_TEACHER".equals(role) && course.getTeacherId().equals(userId)) {
            return;
        }
        if ("ROLE_STUDENT".equals(role) && isEnrolled(courseId, userId)) {
            return;
        }
        throw BusinessException.forbidden("无权访问该课程知识库");
    }

    private void checkManageable(Long courseId, Long userId, String role) {
        Course course = requireCourse(courseId);
        if ("ROLE_ADMIN".equals(role)) {
            return;
        }
        if ("ROLE_TEACHER".equals(role) && course.getTeacherId().equals(userId)) {
            return;
        }
        throw BusinessException.forbidden("只能维护自己负责课程的知识库");
    }

    private boolean isEnrolled(Long courseId, Long studentId) {
        if (courseId == null || studentId == null) {
            return false;
        }
        return courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStudentId, studentId)) > 0;
    }

    private String snippet(String content, int length) {
        if (content == null) {
            return "";
        }
        return content.length() > length ? content.substring(0, length) + "..." : content;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @Data
    private static class KnowledgeSource {
        private String title;
        private String content;
        private String sourceType;
        private Long sourceId;
        private String sourceUrl;
    }
}
