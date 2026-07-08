package com.kechang.vo;

import com.kechang.entity.AttachmentFile;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AssignmentSubmitVO {

    private Long id;
    private Long assignmentId;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private String studentMajor;
    private String className;
    private String content;
    private String attachmentUrl;
    private List<AttachmentFile> attachments;
    private List<AttachmentFile> feedbackAttachments;
    private LocalDateTime submitTime;
    private Double score;
    private String comment;
    private String feedbackMarks;
    private LocalDateTime gradedTime;
    private String status;
    private LocalDateTime createTime;
}
