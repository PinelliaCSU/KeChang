package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kechang.common.BusinessException;
import com.kechang.entity.VideoAnnotation;
import com.kechang.mapper.VideoAnnotationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VideoAnnotationService {

    private final VideoAnnotationMapper videoAnnotationMapper;

    public List<Map<String, Object>> list(Long userId, String fileKey) {
        String hash = checkedHash(userId, fileKey);
        return videoAnnotationMapper.selectList(new LambdaQueryWrapper<VideoAnnotation>()
                        .eq(VideoAnnotation::getUserId, userId)
                        .eq(VideoAnnotation::getFileKeyHash, hash)
                        .orderByAsc(VideoAnnotation::getTimeSeconds)
                        .orderByAsc(VideoAnnotation::getCreateTime))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(Long userId, String fileKey, List<Map<String, Object>> annotations) {
        String hash = checkedHash(userId, fileKey);
        videoAnnotationMapper.delete(new LambdaQueryWrapper<VideoAnnotation>()
                .eq(VideoAnnotation::getUserId, userId)
                .eq(VideoAnnotation::getFileKeyHash, hash));
        if (annotations == null || annotations.isEmpty()) {
            return;
        }
        annotations.stream()
                .filter(item -> StringUtils.hasText(stringValue(item.get("text"))))
                .sorted((left, right) -> decimalValue(left.get("time")).compareTo(decimalValue(right.get("time"))))
                .forEach(item -> videoAnnotationMapper.insert(toEntity(userId, fileKey, hash, item)));
    }

    private Map<String, Object> toDto(VideoAnnotation annotation) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", String.valueOf(annotation.getId()));
        dto.put("time", annotation.getTimeSeconds() == null ? 0 : annotation.getTimeSeconds().stripTrailingZeros());
        dto.put("text", annotation.getContent());
        dto.put("author", annotation.getAuthor());
        dto.put("createdAt", annotation.getCreateTime() == null
                ? null
                : annotation.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toString());
        return dto;
    }

    private VideoAnnotation toEntity(Long userId, String fileKey, String hash, Map<String, Object> item) {
        VideoAnnotation annotation = new VideoAnnotation();
        annotation.setUserId(userId);
        annotation.setFileKeyHash(hash);
        annotation.setFileKey(fileKey);
        annotation.setTimeSeconds(decimalValue(item.get("time")));
        annotation.setContent(stringValue(item.get("text")));
        annotation.setAuthor(stringValue(item.get("author")));
        return annotation;
    }

    private String checkedHash(Long userId, String fileKey) {
        if (userId == null) {
            throw BusinessException.unauthorized("请先登录");
        }
        if (!StringUtils.hasText(fileKey)) {
            throw BusinessException.badRequest("文件标识不能为空");
        }
        return sha256(fileKey);
    }

    private BigDecimal decimalValue(Object value) {
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).max(BigDecimal.ZERO);
        }
        try {
            return new BigDecimal(String.valueOf(value)).max(BigDecimal.ZERO);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException("生成文件批注索引失败");
        }
    }
}
