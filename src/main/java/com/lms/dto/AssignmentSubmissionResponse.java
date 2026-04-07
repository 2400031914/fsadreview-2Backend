package com.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AssignmentSubmissionResponse {
    private Long id;
    private String fileName;
    private Long studentId;
    private Long courseId;
    private LocalDateTime submittedAt;
}
