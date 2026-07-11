package com.kechang.service.vector;

import lombok.Data;

import java.util.List;

@Data
public class VectorRecord {
    private Long courseId;
    private Long documentId;
    private Long chunkId;
    private List<Double> vector;
}
