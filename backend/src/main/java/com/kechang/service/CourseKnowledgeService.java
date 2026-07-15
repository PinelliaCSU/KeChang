package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final CourseKnowledgeQaHistoryMapper qaHistoryMapper;
    private final FileProcessingService fileProcessingService;
    private final BailianApiService bailianApiService;
    private final CourseVectorStore vectorStore;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

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
            saveQaHistory(dto.getCourseId(), userId, dto.getQuestion(), vo);
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
                    .append(chunk.getSourceTitle()).append(" / ").append(chunk.getSourceType())
                    .append(sourceLocator(chunk)).append("\n")
                    .append(chunk.getContent()).append("\n\n");
            CourseKnowledgeAskVO.Reference ref = new CourseKnowledgeAskVO.Reference();
            ref.setChunkId(chunk.getId());
            ref.setDocumentId(chunk.getDocumentId());
            ref.setTitle(chunk.getSourceTitle());
            ref.setSourceType(chunk.getSourceType());
            ref.setSourceUrl(chunk.getSourceUrl());
            ref.setPageNo(chunk.getPageNo());
            ref.setStartTime(chunk.getStartTime());
            ref.setEndTime(chunk.getEndTime());
            ref.setSectionTitle(chunk.getSectionTitle());
            ref.setMetadataJson(chunk.getMetadataJson());
            ref.setScore(scoreMap.getOrDefault(chunk.getId(), 0.0));
            ref.setSnippet(snippet(chunk.getContent(), 180));
            references.add(ref);
            index++;
        }

        CourseKnowledgeAskVO vo = new CourseKnowledgeAskVO();
        vo.setAnswer(bailianApiService.answerWithContext(dto.getQuestion(), context.toString(), course.getCourseName()));
        vo.setReferences(references);
        saveQaHistory(dto.getCourseId(), userId, dto.getQuestion(), vo);
        return vo;
    }

    public List<CourseKnowledgeQaHistory> listHistory(Long courseId, Long userId, String role) {
        checkReadable(courseId, userId, role);
        return qaHistoryMapper.selectList(new LambdaQueryWrapper<CourseKnowledgeQaHistory>()
                .eq(CourseKnowledgeQaHistory::getCourseId, courseId)
                .eq(CourseKnowledgeQaHistory::getUserId, userId)
                .orderByDesc(CourseKnowledgeQaHistory::getCreateTime)
                .last("limit 30"));
    }

    private void saveQaHistory(Long courseId, Long userId, String question, CourseKnowledgeAskVO vo) {
        if (courseId == null || userId == null || !StringUtils.hasText(question) || vo == null) {
            return;
        }
        CourseKnowledgeQaHistory history = new CourseKnowledgeQaHistory();
        history.setCourseId(courseId);
        history.setUserId(userId);
        history.setQuestion(question);
        history.setAnswer(vo.getAnswer());
        history.setReferencesJson(toJsonValue(vo.getReferences()));
        qaHistoryMapper.insert(history);
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
            List<KnowledgeChunk> chunks = buildChunks(source);
            int index = 0;
            for (KnowledgeChunk knowledgeChunk : chunks) {
                String text = knowledgeChunk.getContent();
                CourseKnowledgeChunk chunk = new CourseKnowledgeChunk();
                chunk.setDocumentId(doc.getId());
                chunk.setCourseId(courseId);
                chunk.setChunkIndex(index);
                chunk.setContent(text);
                chunk.setSourceTitle(source.getTitle());
                chunk.setSourceType(source.getSourceType());
                chunk.setSourceId(source.getSourceId());
                chunk.setSourceUrl(source.getSourceUrl());
                chunk.setPageNo(knowledgeChunk.getPageNo());
                chunk.setStartTime(knowledgeChunk.getStartTime());
                chunk.setEndTime(knowledgeChunk.getEndTime());
                chunk.setSectionTitle(knowledgeChunk.getSectionTitle());
                chunk.setTokenCount(roughTokenCount(text));
                chunk.setMetadataJson(toJson(mergedMetadata(source, knowledgeChunk)));
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
                    String ext = extension(resource.getResourceName(), resource.getFileUrl());
                    if (!StringUtils.hasText(content)) {
                        content = "资源名称：" + resource.getResourceName() + "\n类型：" + resource.getResourceType();
                    }
                    KnowledgeSource source = new KnowledgeSource();
                    source.setTitle(resource.getResourceName());
                    source.setContent(content);
                    source.setSourceType("RESOURCE");
                    source.setSourceId(resource.getId());
                    source.setSourceUrl(resource.getFileUrl());
                    source.setMetadata(Map.of(
                            "resourceType", safe(resource.getResourceType()),
                            "chapterId", resource.getChapterId() == null ? "" : resource.getChapterId(),
                            "extension", ext));
                    List<FileProcessingService.TextPage> pages = fileProcessingService.extractTextPages(resource.getFileUrl(), resource.getResourceName());
                    if (!pages.isEmpty()) {
                        List<KnowledgeChunk> prebuilt = new ArrayList<>();
                        String pageType = "pptx".equals(ext) ? "slide" : "page";
                        for (FileProcessingService.TextPage page : pages) {
                            Map<String, Object> metadata = new LinkedHashMap<>(source.getMetadata());
                            metadata.put("pageType", pageType);
                            metadata.put("pageNo", page.getPageNo());
                            prebuilt.addAll(semanticChunks(page.getContent(),
                                    ("slide".equals(pageType) ? "第 " + page.getPageNo() + " 页幻灯片" : "第 " + page.getPageNo() + " 页"),
                                    page.getPageNo(),
                                    metadata));
                        }
                        source.setPrebuiltChunks(prebuilt);
                    }
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
                    source.setSectionTitle(item.getTitle());
                    source.setMetadata(Map.of(
                            "assignmentId", item.getId(),
                            "chapterId", item.getChapterId() == null ? "" : item.getChapterId(),
                            "deadline", item.getDeadline() == null ? "" : item.getDeadline().toString(),
                            "totalScore", item.getTotalScore() == null ? "" : item.getTotalScore()));
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
                    source.setSectionTitle("题目 #" + item.getId());
                    source.setMetadata(Map.of(
                            "questionId", item.getId(),
                            "questionType", safe(item.getQuestionType()),
                            "difficulty", item.getDifficulty() == null ? "" : item.getDifficulty(),
                            "knowledgePoint", safe(item.getKnowledgePoint())));
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
            source.setSectionTitle(post.getTitle());
            source.setMetadata(Map.of(
                    "postId", post.getId(),
                    "postType", safe(post.getPostType()),
                    "replyCount", post.getReplyCount() == null ? answers.size() : post.getReplyCount()));
            sources.add(source);
        }
        return sources;
    }

    private List<KnowledgeChunk> buildChunks(KnowledgeSource source) {
        if (source == null || !StringUtils.hasText(source.getContent())) {
            return List.of();
        }
        if (source.getPrebuiltChunks() != null && !source.getPrebuiltChunks().isEmpty()) {
            return source.getPrebuiltChunks();
        }
        String type = source.getSourceType();
        if ("QUESTION".equals(type) || "QA".equals(type) || "ASSIGNMENT".equals(type) || "MANUAL".equals(type)) {
            return semanticChunks(source.getContent(), source.getSectionTitle(), null, source.getMetadata());
        }
        if ("RESOURCE".equals(type)) {
            String ext = String.valueOf(source.getMetadata().getOrDefault("extension", "")).toLowerCase(Locale.ROOT);
            if (isMediaExtension(ext)) {
                return mediaChunks(source);
            }
            if ("pptx".equals(ext) || source.getContent().contains("\n---\n")) {
                return slideChunks(source);
            }
        }
        return semanticChunks(source.getContent(), source.getSectionTitle(), null, source.getMetadata());
    }

    private List<KnowledgeChunk> mediaChunks(KnowledgeSource source) {
        List<KnowledgeChunk> chunks = semanticChunks(source.getContent(), source.getSectionTitle(), null, source.getMetadata());
        int secondsPerChunk = 120;
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = chunks.get(i);
            chunk.setStartTime(i * secondsPerChunk);
            chunk.setEndTime((i + 1) * secondsPerChunk);
            Map<String, Object> metadata = new LinkedHashMap<>(chunk.getMetadata());
            metadata.put("timeSource", "estimated");
            metadata.put("estimatedSecondsPerChunk", secondsPerChunk);
            chunk.setMetadata(metadata);
        }
        return chunks;
    }

    private List<KnowledgeChunk> slideChunks(KnowledgeSource source) {
        List<KnowledgeChunk> chunks = new ArrayList<>();
        String[] slides = source.getContent().split("\\n---\\n");
        for (int i = 0; i < slides.length; i++) {
            String text = slides[i].trim();
            if (!StringUtils.hasText(text)) {
                continue;
            }
            Map<String, Object> metadata = new LinkedHashMap<>(source.getMetadata());
            metadata.put("pageType", "slide");
            metadata.put("pageNo", i + 1);
            chunks.addAll(semanticChunks(text, "第 " + (i + 1) + " 页幻灯片", i + 1, metadata));
        }
        return chunks;
    }

    private List<KnowledgeChunk> semanticChunks(String content, String defaultSection, Integer pageNo, Map<String, Object> metadata) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        String clean = content.replace("\r", "\n").replaceAll("\\n{3,}", "\n\n").trim();
        int size = 900;
        int overlap = 120;
        List<KnowledgeChunk> chunks = new ArrayList<>();
        String[] paragraphs = clean.split("\\n\\s*\\n");
        StringBuilder current = new StringBuilder();
        String currentSection = StringUtils.hasText(defaultSection) ? defaultSection : inferSectionTitle(clean);
        for (String paragraph : paragraphs) {
            String text = paragraph.trim();
            if (!StringUtils.hasText(text)) {
                continue;
            }
            if (isSectionHeading(text)) {
                flushChunk(chunks, current, currentSection, pageNo, metadata);
                currentSection = text.length() > 120 ? text.substring(0, 120) : text;
                continue;
            }
            if (text.length() > size) {
                flushChunk(chunks, current, currentSection, pageNo, metadata);
                splitLongText(text, size, overlap, chunks, currentSection, pageNo, metadata);
                continue;
            }
            if (current.length() > 0 && current.length() + text.length() + 2 > size) {
                flushChunk(chunks, current, currentSection, pageNo, metadata);
            }
            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(text);
        }
        flushChunk(chunks, current, currentSection, pageNo, metadata);
        return chunks;
    }

    private void flushChunk(List<KnowledgeChunk> chunks, StringBuilder current, String sectionTitle, Integer pageNo, Map<String, Object> metadata) {
        String text = current.toString().trim();
        if (text.length() >= 20) {
            chunks.add(toChunk(text, sectionTitle, pageNo, metadata));
        }
        current.setLength(0);
    }

    private void splitLongText(String text, int size, int overlap, List<KnowledgeChunk> chunks, String sectionTitle, Integer pageNo, Map<String, Object> metadata) {
        int step = Math.max(1, size - overlap);
        for (int start = 0; start < text.length(); start += step) {
            int end = Math.min(text.length(), start + size);
            String part = text.substring(start, end).trim();
            if (part.length() >= 20) {
                Map<String, Object> splitMetadata = new LinkedHashMap<>(metadata == null ? Map.of() : metadata);
                splitMetadata.put("splitStart", start);
                splitMetadata.put("splitEnd", end);
                chunks.add(toChunk(part, sectionTitle, pageNo, splitMetadata));
            }
            if (end >= text.length()) {
                break;
            }
        }
    }

    private KnowledgeChunk toChunk(String content, String sectionTitle, Integer pageNo, Map<String, Object> metadata) {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setContent(content);
        chunk.setPageNo(pageNo);
        chunk.setSectionTitle(StringUtils.hasText(sectionTitle) ? sectionTitle : inferSectionTitle(content));
        chunk.setMetadata(metadata == null ? Map.of() : metadata);
        return chunk;
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
            return hasRequiredKnowledgeColumns();
        } catch (DataAccessException e) {
            return false;
        }
    }

    private boolean hasRequiredKnowledgeColumns() {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                            SELECT COUNT(1)
                            FROM information_schema.COLUMNS
                            WHERE TABLE_SCHEMA = DATABASE()
                              AND TABLE_NAME = 'course_knowledge_chunk'
                              AND COLUMN_NAME IN ('page_no', 'start_time', 'end_time', 'section_title', 'token_count', 'metadata_json')
                            """,
                    Integer.class);
            return count != null && count == 6;
        } catch (DataAccessException e) {
            return false;
        }
    }

    private void ensureKnowledgeStorageReady() {
        if (!isKnowledgeStorageReady()) {
            throw BusinessException.badRequest("课程知识库表未初始化或字段不完整，请先执行 database/migration_add_course_knowledge_rag.sql 和 database/migration_add_course_knowledge_chunk_metadata.sql");
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

    private String sourceLocator(CourseKnowledgeChunk chunk) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(chunk.getSectionTitle())) {
            parts.add(chunk.getSectionTitle());
        }
        if (chunk.getPageNo() != null) {
            parts.add("第 " + chunk.getPageNo() + " 页");
        }
        if (chunk.getStartTime() != null || chunk.getEndTime() != null) {
            parts.add(formatSeconds(chunk.getStartTime()) + "-" + formatSeconds(chunk.getEndTime()));
        }
        return parts.isEmpty() ? "" : "（" + String.join("，", parts) + "）";
    }

    private String formatSeconds(Integer seconds) {
        if (seconds == null || seconds < 0) {
            return "--:--";
        }
        int minute = seconds / 60;
        int second = seconds % 60;
        return String.format(Locale.ROOT, "%02d:%02d", minute, second);
    }

    private boolean isSectionHeading(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String line = text.trim();
        if (line.length() > 80 || line.contains("。") || line.contains("，")) {
            return false;
        }
        return line.matches("^(第[一二三四五六七八九十百0-9]+[章节节课部分].*)$")
                || line.matches("^[0-9]+(\\.[0-9]+)*\\s+.+$")
                || line.startsWith("#");
    }

    private String inferSectionTitle(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        return Arrays.stream(content.split("\\R"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(line -> line.length() <= 80)
                .findFirst()
                .orElse(null);
    }

    private int roughTokenCount(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        int asciiWords = 0;
        int cjkChars = 0;
        boolean inAsciiWord = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                cjkChars++;
                inAsciiWord = false;
            } else if (Character.isLetterOrDigit(c)) {
                if (!inAsciiWord) {
                    asciiWords++;
                    inAsciiWord = true;
                }
            } else {
                inAsciiWord = false;
            }
        }
        return cjkChars + asciiWords;
    }

    private Map<String, Object> mergedMetadata(KnowledgeSource source, KnowledgeChunk chunk) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (source.getMetadata() != null) {
            metadata.putAll(source.getMetadata());
        }
        if (chunk.getMetadata() != null) {
            metadata.putAll(chunk.getMetadata());
        }
        metadata.put("sourceType", source.getSourceType());
        metadata.put("sourceId", source.getSourceId());
        return metadata;
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String toJsonValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String extension(String name, String fallback) {
        String value = StringUtils.hasText(name) ? name : fallback;
        if (!StringUtils.hasText(value)) {
            return "";
        }
        int query = value.indexOf('?');
        if (query >= 0) {
            value = value.substring(0, query);
        }
        int slash = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        if (slash >= 0) {
            value = value.substring(slash + 1);
        }
        int dot = value.lastIndexOf('.');
        return dot >= 0 && dot < value.length() - 1 ? value.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private boolean isMediaExtension(String ext) {
        return Set.of("mp4", "webm", "ogg", "mov", "m4v", "avi", "mkv", "flv", "wmv",
                "mp3", "wav", "m4a", "aac", "flac", "wma").contains(ext);
    }

    @Data
    private static class KnowledgeSource {
        private String title;
        private String content;
        private String sourceType;
        private Long sourceId;
        private String sourceUrl;
        private String sectionTitle;
        private Map<String, Object> metadata = Map.of();
        private List<KnowledgeChunk> prebuiltChunks = List.of();
    }

    @Data
    private static class KnowledgeChunk {
        private String content;
        private Integer pageNo;
        private Integer startTime;
        private Integer endTime;
        private String sectionTitle;
        private Map<String, Object> metadata = Map.of();
    }
}
