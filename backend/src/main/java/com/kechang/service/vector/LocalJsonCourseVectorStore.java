package com.kechang.service.vector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
@RequiredArgsConstructor
public class LocalJsonCourseVectorStore implements CourseVectorStore {

    private final ObjectMapper objectMapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Value("${kechang.upload.path:./uploads}")
    private String uploadBasePath;

    @Override
    public void upsert(VectorRecord record) {
        lock.writeLock().lock();
        try {
            List<VectorRecord> records = load();
            records.removeIf(item -> item.getChunkId() != null && item.getChunkId().equals(record.getChunkId()));
            records.add(record);
            save(records);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void deleteByCourse(Long courseId) {
        lock.writeLock().lock();
        try {
            List<VectorRecord> records = load();
            records.removeIf(item -> courseId != null && courseId.equals(item.getCourseId()));
            save(records);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void deleteByDocument(Long documentId) {
        lock.writeLock().lock();
        try {
            List<VectorRecord> records = load();
            records.removeIf(item -> documentId != null && documentId.equals(item.getDocumentId()));
            save(records);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<VectorSearchResult> search(Long courseId, List<Double> queryVector, int topK) {
        lock.readLock().lock();
        try {
            if (queryVector == null || queryVector.isEmpty()) {
                return List.of();
            }
            return load().stream()
                    .filter(item -> courseId != null && courseId.equals(item.getCourseId()))
                    .map(item -> {
                        VectorSearchResult result = new VectorSearchResult();
                        result.setChunkId(item.getChunkId());
                        result.setScore(cosine(queryVector, item.getVector()));
                        return result;
                    })
                    .filter(item -> item.getScore() != null)
                    .sorted(Comparator.comparing(VectorSearchResult::getScore).reversed())
                    .limit(Math.max(1, topK))
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    private List<VectorRecord> load() {
        Path file = storeFile();
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(file.toFile(), new TypeReference<List<VectorRecord>>() {
            });
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private void save(List<VectorRecord> records) {
        Path file = storeFile();
        try {
            Files.createDirectories(file.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), records);
        } catch (IOException e) {
            throw new IllegalStateException("保存课程知识库向量失败：" + e.getMessage(), e);
        }
    }

    private Path storeFile() {
        return Paths.get(uploadBasePath).toAbsolutePath().normalize()
                .resolve("vector-store")
                .resolve("course-knowledge-vectors.json");
    }

    private Double cosine(List<Double> a, List<Double> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return null;
        }
        int size = Math.min(a.size(), b.size());
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < size; i++) {
            double va = a.get(i) == null ? 0.0 : a.get(i);
            double vb = b.get(i) == null ? 0.0 : b.get(i);
            dot += va * vb;
            normA += va * va;
            normB += vb * vb;
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
