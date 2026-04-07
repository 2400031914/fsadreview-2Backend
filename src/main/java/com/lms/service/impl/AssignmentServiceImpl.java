package com.lms.service.impl;

import com.lms.dto.AssignmentSubmissionResponse;
import com.lms.exception.BadRequestException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.model.AssignmentSubmission;
import com.lms.repository.AssignmentSubmissionRepository;
import com.lms.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    @Transactional
    public AssignmentSubmissionResponse uploadAssignment(MultipartFile file, Long studentId, Long courseId) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "submission.bin";
            String safeFileName = UUID.randomUUID() + "_" + originalName.replaceAll("[\\\\/:*?\"<>|]", "_");
            Path targetPath = uploadPath.resolve(safeFileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            AssignmentSubmission submission = AssignmentSubmission.builder()
                    .fileName(originalName)
                    .filePath(targetPath.toString())
                    .studentId(studentId)
                    .courseId(courseId)
                    .submittedAt(LocalDateTime.now())
                    .build();

            AssignmentSubmission saved = assignmentSubmissionRepository.save(submission);
            log.info("Assignment uploaded: submissionId={}, studentId={}, courseId={}", saved.getId(), studentId, courseId);
            return toResponse(saved);
        } catch (Exception ex) {
            throw new BadRequestException("Failed to upload assignment file");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> getSubmissionsByCourse(Long courseId) {
        return assignmentSubmissionRepository.findByCourseIdOrderBySubmittedAtDesc(courseId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadSubmission(Long submissionId) {
        AssignmentSubmission submission = assignmentSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));
        try {
            Path filePath = Paths.get(submission.getFilePath()).toAbsolutePath().normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("File not found");
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new BadRequestException("Invalid file path");
        }
    }

    private AssignmentSubmissionResponse toResponse(AssignmentSubmission submission) {
        return AssignmentSubmissionResponse.builder()
                .id(submission.getId())
                .fileName(submission.getFileName())
                .studentId(submission.getStudentId())
                .courseId(submission.getCourseId())
                .submittedAt(submission.getSubmittedAt())
                .build();
    }
}
