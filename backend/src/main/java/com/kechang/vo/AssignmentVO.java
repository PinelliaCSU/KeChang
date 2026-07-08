package com.kechang.vo;

import com.kechang.entity.AttachmentFile;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AssignmentVO {

    private Long id;
    private Long courseId;
    private Long chapterId;
    private String courseName;
    private String title;
    private String content;
    private String attachmentUrl;
    private List<AttachmentFile> attachments;
    private LocalDateTime deadline;
    private Double totalScore;
    private String status;
    private Long createBy;
    private LocalDateTime createTime;
    private Integer submitCount;
    private Integer totalStudents;
    private Boolean submitted;
    private Long mySubmitId;
    private String submitContent;
    private String submitAttachmentUrl;
    private List<AttachmentFile> submitAttachments;
    private LocalDateTime submitTime;
    private String mySubmitStatus;
    private Double myScore;
    private String gradeComment;
    private LocalDateTime gradedTime;
}
