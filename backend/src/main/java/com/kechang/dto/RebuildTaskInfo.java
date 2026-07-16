package com.kechang.dto;

import lombok.Data;

/**
 * 知识库异步重建任务状态。
 */
@Data
public class RebuildTaskInfo {

    public enum Status {
        RUNNING, DONE, FAILED
    }

    private String taskId;
    private Long courseId;
    private Long teacherId;
    private Status status = Status.RUNNING;
    private int documents;
    private int chunks;
    private String error;
    private long startTime;
    private long endTime;
}
