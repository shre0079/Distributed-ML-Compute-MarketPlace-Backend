package com.dcm.backend.demo.repository;

import com.dcm.backend.demo.dto.Job;
import com.dcm.backend.demo.enums.JobStatus;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends Repository<Job, String> {
    Optional<Job> findFirstByStatus(JobStatus status);

    Job save(Job job);

    Optional<Object> findById(String jobId);

    List<Job> findAll();

    List<Job> findAllByStatus(JobStatus status);
}
