package com.kechang.service;

import com.kechang.dto.RebuildTaskInfo;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存级别的重建任务状态管理器。 重启后丢失，轻量无依赖。
 */
@Component
public class RebuildTaskManager {

    private final Map<String, RebuildTaskInfo> tasks = new ConcurrentHashMap<>();

    /**
     * 创建一个新任务并返回 taskId
     */
    public String create(Long courseId, Long teacherId) {
        String taskId = UUID.randomUUID().toString();
        RebuildTaskInfo info = new RebuildTaskInfo();
        info.setTaskId(taskId);
        info.setCourseId(courseId);
        info.setTeacherId(teacherId);
        info.setStartTime(System.currentTimeMillis());
        tasks.put(taskId, info);
        return taskId;
    }

    /**
     * 获取任务状态，不存在返回 null
     */
    public RebuildTaskInfo get(String taskId) {
        return tasks.get(taskId);
    }

    /**
     * 标记任务完成
     */
    public void markDone(String taskId, int documents, int chunks) {
        RebuildTaskInfo info = tasks.get(taskId);
        if (info != null) {
            info.setDocuments(documents);
            info.setChunks(chunks);
            info.setStatus(RebuildTaskInfo.Status.DONE);
            info.setEndTime(System.currentTimeMillis());
        }
    }

    /**
     * 标记任务失败
     */
    public void markFailed(String taskId, String error) {
        RebuildTaskInfo info = tasks.get(taskId);
        if (info != null) {
            info.setStatus(RebuildTaskInfo.Status.FAILED);
            info.setError(error);
            info.setEndTime(System.currentTimeMillis());
        }
    }

    /**
     * 清理 24 小时前的已完成/失败任务
     */
    public void cleanup() {
        long threshold = System.currentTimeMillis() - 24 * 3600_000L;
        tasks.entrySet().removeIf(e -> {
            RebuildTaskInfo info = e.getValue();
            return info.getStatus() != RebuildTaskInfo.Status.RUNNING && info.getEndTime() < threshold;
        });
    }
}
