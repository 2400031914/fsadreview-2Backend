package com.lms.controller;

import com.lms.dto.AssignmentSubmissionResponse;
import com.lms.service.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
@Tag(name = "Assignments", description = "Student submissions upload and instructor/admin access")
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload assignment", description = "Student uploads assignment file with studentId and courseId (requires JWT role STUDENT).")
    public ResponseEntity<AssignmentSubmissionResponse> uploadAssignment(
            @RequestParam("file") MultipartFile file,
            @RequestParam("studentId") Long studentId,
            @RequestParam("courseId") Long courseId
    ) {
        return ResponseEntity.ok(assignmentService.uploadAssignment(file, studentId, courseId));
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "View submissions by course", description = "Instructor/Admin views submissions for a course (requires JWT).")
    public ResponseEntity<List<AssignmentSubmissionResponse>> getByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(assignmentService.getSubmissionsByCourse(courseId));
    }

    @GetMapping("/download/{id}")
    @Operation(summary = "Download submission file", description = "Instructor/Admin downloads a submitted file by submission id (requires JWT).")
    public ResponseEntity<Resource> download(@PathVariable("id") Long id) {
        Resource resource = assignmentService.downloadSubmission(id);
        String fileName = resource.getFilename() != null ? resource.getFilename() : "submission";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
