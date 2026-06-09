package com.dcm.backend.demo.repository;

import com.dcm.backend.demo.dto.entity.Job;
import com.dcm.backend.demo.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface JobRepository extends JpaRepository<Job, String> {

    Optional<Job> findFirstByStatus(JobStatus status);

    List<Job> findAllByStatus(JobStatus status);

    List<Job> findAllByWorkerIdAndStatus(String workerId, JobStatus status);
}
