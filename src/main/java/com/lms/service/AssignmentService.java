package com.lms.service;

import com.lms.dto.AssignmentSubmissionResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AssignmentService {
    AssignmentSubmissionResponse uploadAssignment(MultipartFile file, Long studentId, Long courseId);
    List<AssignmentSubmissionResponse> getSubmissionsByCourse(Long courseId);
    Resource downloadSubmission(Long submissionId);
}
