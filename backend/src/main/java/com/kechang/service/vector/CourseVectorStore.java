package com.kechang.service.vector;

import java.util.List;

public interface CourseVectorStore {

    void upsert(VectorRecord record);

    void deleteByCourse(Long courseId);

    void deleteByDocument(Long documentId);

    List<VectorSearchResult> search(Long courseId, List<Double> queryVector, int topK);
}
