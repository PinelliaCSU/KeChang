package com.kechang.service.vector;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kechang.rag.vector-store", havingValue = "redis")
public class RedisCourseVectorStore implements CourseVectorStore {

    private static final String INDEX = "idx:course_knowledge_vectors";
    private static final String KEY_PREFIX = "kc:rag:chunk:";
    private static final String DIM_KEY = "kc:rag:vector:dim";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void upsert(VectorRecord record) {
        upsertBatch(List.of(record));
    }

    @Override
    public void upsertBatch(List<VectorRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        // 用第一个记录的向量维度建索引
        VectorRecord first = records.stream()
                .filter(r -> !CollectionUtils.isEmpty(r.getVector()))
                .findFirst().orElse(null);
        if (first == null) {
            return;
        }
        ensureIndex(first.getVector().size());

        redisTemplate.executePipelined((RedisConnection connection) -> {
            for (VectorRecord record : records) {
                if (record.getChunkId() == null || CollectionUtils.isEmpty(record.getVector())) {
                    continue;
                }
                connection.execute("HSET",
                        bytes(key(record.getChunkId())),
                        bytes("course_id"), bytes(record.getCourseId()),
                        bytes("document_id"), bytes(record.getDocumentId()),
                        bytes("chunk_id"), bytes(record.getChunkId()),
                        bytes("vector"), vectorBytes(record.getVector()));
            }
            return null;
        });
    }

    @Override
    public void deleteByCourse(Long courseId) {
        deleteByQuery("@course_id:{" + courseId + "}");
    }

    @Override
    public void deleteByDocument(Long documentId) {
        deleteByQuery("@document_id:{" + documentId + "}");
    }

    @Override
    public List<VectorSearchResult> search(Long courseId, List<Double> queryVector, int topK) {
        if (courseId == null || CollectionUtils.isEmpty(queryVector)) {
            return List.of();
        }
        ensureIndex(queryVector.size());
        String query = "(@course_id:{" + courseId + "})=>[KNN " + Math.max(1, topK) + " @vector $vec AS score]";
        Object raw = execute("FT.SEARCH",
                bytes(INDEX),
                bytes(query),
                bytes("PARAMS"), bytes("2"), bytes("vec"), vectorBytes(queryVector),
                bytes("RETURN"), bytes("4"), bytes("chunk_id"), bytes("document_id"), bytes("course_id"), bytes("score"),
                bytes("SORTBY"), bytes("score"),
                bytes("DIALECT"), bytes("2"));
        return parseSearchResults(raw);
    }

    private void ensureIndex(int dimension) {
        String existing = redisTemplate.opsForValue().get(DIM_KEY);
        if (existing != null) {
            int dim = Integer.parseInt(existing);
            if (dim != dimension) {
                throw new IllegalStateException("Redis Vector 索引维度不一致：当前索引 " + dim + "，请求向量 " + dimension);
            }
            return;
        }
        try {
            execute("FT.CREATE",
                    bytes(INDEX),
                    bytes("ON"), bytes("HASH"),
                    bytes("PREFIX"), bytes("1"), bytes(KEY_PREFIX),
                    bytes("SCHEMA"),
                    bytes("course_id"), bytes("TAG"),
                    bytes("document_id"), bytes("TAG"),
                    bytes("chunk_id"), bytes("TAG"),
                    bytes("vector"), bytes("VECTOR"), bytes("HNSW"), bytes("6"),
                    bytes("TYPE"), bytes("FLOAT32"),
                    bytes("DIM"), bytes(String.valueOf(dimension)),
                    bytes("DISTANCE_METRIC"), bytes("COSINE"));
        } catch (RuntimeException e) {
            String message = e.getMessage() == null ? "" : e.getMessage();
            if (!message.contains("Index already exists")) {
                throw new IllegalStateException("Redis Vector 索引创建失败，请确认使用 redis/redis-stack-server 镜像并启用 RediSearch：" + message, e);
            }
        }
        redisTemplate.opsForValue().set(DIM_KEY, String.valueOf(dimension));
    }

    private void deleteByQuery(String query) {
        try {
            Object raw = execute("FT.SEARCH",
                    bytes(INDEX),
                    bytes(query),
                    bytes("RETURN"), bytes("0"),
                    bytes("LIMIT"), bytes("0"), bytes("10000"),
                    bytes("DIALECT"), bytes("2"));
            Set<String> keys = parseKeys(raw);
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (RuntimeException e) {
            if (!isMissingIndex(e)) {
                throw e;
            }
        }
    }

    private List<VectorSearchResult> parseSearchResults(Object raw) {
        if (!(raw instanceof List<?> rows) || rows.size() <= 1) {
            return List.of();
        }
        List<VectorSearchResult> results = new ArrayList<>();
        for (int i = 1; i + 1 < rows.size(); i += 2) {
            Object fields = rows.get(i + 1);
            if (!(fields instanceof List<?> fieldList)) {
                continue;
            }
            Long chunkId = null;
            Double distance = null;
            for (int j = 0; j + 1 < fieldList.size(); j += 2) {
                String name = string(fieldList.get(j));
                String value = string(fieldList.get(j + 1));
                if ("chunk_id".equals(name)) {
                    chunkId = parseLong(value);
                } else if ("score".equals(name)) {
                    distance = parseDouble(value);
                }
            }
            if (chunkId != null) {
                VectorSearchResult result = new VectorSearchResult();
                result.setChunkId(chunkId);
                result.setScore(distance == null ? 0.0 : Math.max(0.0, 1.0 - distance));
                results.add(result);
            }
        }
        return results;
    }

    private Set<String> parseKeys(Object raw) {
        Set<String> keys = new LinkedHashSet<>();
        if (!(raw instanceof List<?> rows) || rows.size() <= 1) {
            return keys;
        }
        for (int i = 1; i < rows.size(); i++) {
            keys.add(string(rows.get(i)));
        }
        return keys;
    }

    private Object execute(String command, byte[]... args) {
        try {
            return redisTemplate.execute((RedisConnection connection) -> connection.execute(command, args));
        } catch (RedisSystemException e) {
            throw new IllegalStateException("Redis Vector 命令执行失败：" + e.getMessage(), e);
        }
    }

    private boolean isMissingIndex(RuntimeException e) {
        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        return message.contains("unknown index") || message.contains("no such index");
    }

    private byte[] vectorBytes(List<Double> vector) {
        ByteBuffer buffer = ByteBuffer.allocate(vector.size() * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (Double value : vector) {
            buffer.putFloat(value == null ? 0.0f : value.floatValue());
        }
        return buffer.array();
    }

    private String key(Long chunkId) {
        return KEY_PREFIX + chunkId;
    }

    private byte[] bytes(Object value) {
        return String.valueOf(value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
    }

    private String string(Object value) {
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return String.valueOf(value);
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return null;
        }
    }
}
