package com.kechang.service.vector;

import lombok.Data;

@Data
public class VectorSearchResult {
    private Long chunkId;
    private Double score;
}
