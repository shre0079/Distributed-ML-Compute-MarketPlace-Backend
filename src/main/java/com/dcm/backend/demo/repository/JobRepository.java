package com.dcm.backend.demo.repository;

import com.dcm.backend.demo.dto.entity.Job;
import com.dcm.backend.demo.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, String> {

    // Existing non-paginated methods — still used internally by schedulers
    List<Job> findAllByStatus(JobStatus status);
    List<Job> findAllByWorkerIdAndStatus(String workerId, JobStatus status);
    List<Job> findAllByTargetWorkerIdAndStatus(String targetWorkerId, JobStatus status);
    List<Job> findAllByTargetWorkerIdOrderByCreatedAtDesc(String targetWorkerId);
    int countByWorkerIdAndStatus(String workerId, JobStatus status);

    // New — paginated versions for user-facing and admin endpoints
    Page<Job> findAllByUserId(String userId, Pageable pageable);
    Page<Job> findAllByUserIdAndStatus(String userId, JobStatus status, Pageable pageable);
    Page<Job> findAllByStatus(JobStatus status, Pageable pageable);
}