package com.lms.repository;

import com.lms.model.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {
    List<AssignmentSubmission> findByCourseIdOrderBySubmittedAtDesc(Long courseId);
}
