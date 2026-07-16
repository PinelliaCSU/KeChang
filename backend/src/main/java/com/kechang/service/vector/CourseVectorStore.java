package com.kechang.service.vector;

import java.util.List;

public interface CourseVectorStore {

    void upsert(VectorRecord record);

    /**
     * 批量 upsert，减少 I/O 次数。
     */
    void upsertBatch(List<VectorRecord> records);

    void deleteByCourse(Long courseId);

    void deleteByDocument(Long documentId);

    List<VectorSearchResult> search(Long courseId, List<Double> queryVector, int topK);
}
